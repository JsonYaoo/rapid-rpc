package com.jsonyao.rapid.rpc.registry;

import com.jsonyao.rapid.rpc.client.RpcClient;
import com.jsonyao.rapid.rpc.config.consumer.CachedService;
import com.jsonyao.rapid.rpc.config.consumer.ConsumerConfig;
import com.jsonyao.rapid.rpc.utils.FastJsonConvertUtil;
import com.jsonyao.rapid.rpc.zookeeper.ChangedEvent;
import com.jsonyao.rapid.rpc.zookeeper.NodeListener;
import com.jsonyao.rapid.rpc.zookeeper.ZookeeperClient;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于Netty实现RPC框架: 客户端注册服务 & 服务结点监听
 * => 服务发现的核心类 监听zookeeper的数据节点发生变更，即时的进行感知
 */
public class RpcRegistryConsumerService extends AbstractRpcRegistry implements NodeListener {

    private ZookeeperClient zookeeperClient;

    private final ReentrantLock LOCK = new ReentrantLock();

    /**
     * interfaceClass:version-List<CachedService>
     */
    private ConcurrentHashMap<String, List<CachedService>> CACHED_SERVICES = new ConcurrentHashMap<>();

    /**
     * interfaceClass:version-ConsumerConfig<?>
     */
    private ConcurrentHashMap<String, ConsumerConfig<?>> CACHED_CONSUMER_CONFIGS = new ConcurrentHashMap<>();

    public RpcRegistryConsumerService(ZookeeperClient zookeeperClient) throws Exception {
        this.zookeeperClient = zookeeperClient;

        //	初始化根节点
        if(!zookeeperClient.checkExists(ROOT_PATH)) {
            zookeeperClient.addPersistentNode(ROOT_PATH, ROOT_VALUE);
        }

        //	传入根节点ROOT_PATH 去监听下一级直接子节点
        /**
         * 	/rapid-rpc   --->  	rapid-rpc-1.0.0
         * 		/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.0     => 监听根结点只能得到这里的信息变更
         * 			/providers
         * 				/192.168.11.101:5678
         * 				/192.168.11.102:5679
         * 			/consumers
         * 				/192.168.11.103
         *
         */
        this.zookeeperClient.listener4ChildrenPath(ROOT_PATH, this);
    }

    /**
     * 从缓存中获取ConsumerConfig: urls, proxyInstance, client
     * @param interfaceName
     * @param interfaceVersion
     * @return
     */
    public ConsumerConfig<?> getConsumer(String interfaceName, String interfaceVersion) {
        return CACHED_CONSUMER_CONFIGS.get(interfaceName + ":" + interfaceVersion);
    }

    /**
     * 服务发现 & 服务结点监听: 生成urls, proxyInstance, client
     * @param client
     * @param event
     * @throws Exception
     */
    @Override
    public void nodeChanged(ZookeeperClient client, ChangedEvent event) throws Exception {
        // 结点信息
        String path = event.getPath();
        // 数据信息
        String data = event.getData();
        // 监听类型
        ChangedEvent.Type type = event.getType();

        // 这里只监控添加的操作
        if(ChangedEvent.Type.CHILD_ADDED == type) {
            String[] pathArray = null;
            if(!StringUtils.isBlank(path) && (pathArray = path.substring(1).split("/")).length == 2) {
                //	对根节点下的直接子节点进行继续监听，就是我们的服务权限命名+版本号的路径监听
                //	/rapid-rpc/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.0
                this.zookeeperClient.listener4ChildrenPath(path, this);
            }
            //	继续监听: /rapid-rpc/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.0/providers
            if(!StringUtils.isBlank(path) && (pathArray = path.substring(1).split("/")).length == 3) {
                this.zookeeperClient.listener4ChildrenPath(path, this);
            }
            //	表示服务地址发生了变更, 需要做具体处理: /rapid-rpc/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.0/providers/192.168.11.112
            if(!StringUtils.isBlank(path) && (pathArray = path.substring(1).split("/")).length == 4) {
                // 由于设计两个ConcurrentHashMap的原子操作, 所以要加锁
                LOCK.lock();

                try {
                    /**
                     * pathArray ===>
                     *
                     * rapid-rpc [0]
                     * com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.0  [1]
                     * providers [2]
                     * 192.168.11.112:8080 [3]
                     */
                    String interfaceNameWithV = pathArray[1];
                    String[] arrays = interfaceNameWithV.split(":");
                    String interfaceName = arrays[0];
                    String address = pathArray[3];
                    Map<String, String> instanceMap = FastJsonConvertUtil.convertJSONToObject(data, Map.class);
                    int addressWeight = Integer.parseInt(instanceMap.get("weight"));
                    CachedService cachedService = new CachedService(address, addressWeight);

                    // 设置缓存
                    List<CachedService> addresses = CACHED_SERVICES.get(interfaceNameWithV);
                    if(addresses == null) {
                        CopyOnWriteArrayList<CachedService> newAddresses = new CopyOnWriteArrayList<>();
                        newAddresses.add(cachedService);
                        CACHED_SERVICES.put(interfaceNameWithV, newAddresses);

                        // 初始化ConsumerConfig, 建立Client连接, 生成代理对象
                        ConsumerConfig<?> consumerConfig = new ConsumerConfig<>();
                        consumerConfig.setInterfaceName(interfaceName);
                        CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<>();
                        // 根据权重对address进行加权 => 这里粗略地设置为权重个address
                        for (int i = 0; i < addressWeight; i++) {
                            urls.add(address);
                        }
                        consumerConfig.setUrl(urls);
                        consumerConfig.initRpcClient();
                        CACHED_CONSUMER_CONFIGS.put(interfaceNameWithV, consumerConfig);
                    }
                    // 更新缓存: 由于监听到的事件是增加, 监听的path是服务地址, 所以说明有新的服务地址被添加了, 因此需要更新缓存中的服务列表信息
                    else {
                        addresses.add(cachedService);
                        ConsumerConfig<?> consumerConfig = CACHED_CONSUMER_CONFIGS.get(interfaceNameWithV);
                        RpcClient rpcClient = consumerConfig.getClient();
                        CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<>();
                        for (CachedService service : addresses) {
                            // 根据权重对address进行加权 => 这里粗略地设置为权重个address
                            for (int i = 0; i < service.getWeight(); i++) {
                                urls.add(service.getAddress());
                            }
                        }

                        // 更新consumerConfig的服务列表: 这里是重新构造新的服务列表方式更新
                        consumerConfig.setUrl(urls);

                        // 更新缓存信息, 如果存在还没有连接的服务地址, 则异步发起连接
                        rpcClient.updateConnectedServer(urls);
                    }
                } finally {
                    LOCK.unlock();
                }
            }
        }
    }
}
