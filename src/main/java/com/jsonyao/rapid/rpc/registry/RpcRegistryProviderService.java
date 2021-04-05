package com.jsonyao.rapid.rpc.registry;

import com.jsonyao.rapid.rpc.config.provider.ProviderConfig;
import com.jsonyao.rapid.rpc.utils.FastJsonConvertUtil;
import com.jsonyao.rapid.rpc.zookeeper.ZookeeperClient;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于Netty实现RPC框架: 注册元数据信息服务
 */
public class RpcRegistryProviderService extends AbstractRpcRegistry {

    private ZookeeperClient zookeeperClient;

    public RpcRegistryProviderService(ZookeeperClient zookeeperClient) throws Exception {
        this.zookeeperClient = zookeeperClient;

        // 初始化根结点
        if(!zookeeperClient.checkExists(ROOT_PATH)) {
            zookeeperClient.addPersistentNode(ROOT_PATH, ROOT_VALUE);
        }
    }

    /**
     * 	注册元数据信息到注册中心:
     *
     * 	/rapid-rpc   --->  	rapid-rpc-1.0.0
     * 		/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.0
     * 			/providers
     * 				/192.168.11.101:5678
     * 				/192.168.11.102:5678
     * 		/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.1
     * 			/providers
     * 				/192.168.11.201:1234
     * 				/192.168.11.202:1234
     *
     * @param providerConfig
     *
     */
    public void registry(ProviderConfig providerConfig) throws Exception {
        //	接口命名： com.bfxy.rapid.rpc.invoke.consumer.test.HelloService
        String interfaceName = providerConfig.getInterfaceName();
        //	实例对象：HelloServiceImpl
        Object ref = providerConfig.getRef();
        //	接口对应的版本号：1.0.0
        String version = providerConfig.getVersion();

        //	/rapid-rpc/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.0
        String registryKey = ROOT_PATH + "/" + interfaceName + ":" + version;

        //	如果当前的path不存在 则进行注册到zookeeper
        if(!zookeeperClient.checkExists(registryKey)) {
            /**
             *  @Override
             *  public String hello(String name) {
             *      return "hello! " + name;
             *  }
             *
             *  @Override
             *  public String hello(User user) {
             *      return "hello! " + user.getName();
             *  }
             */
            Method[] methods = ref.getClass().getDeclaredMethods();
            Map<String, String> methodMap = new HashMap<>();
            for (Method method : methods) {
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();

                String methodParameterTypes = "";
                for (Class<?> clazz : parameterTypes) {
                    String parameterTypeName = clazz.getName();
                    methodParameterTypes += parameterTypeName + ",";
                }

                //	hello@com.bfxy.rapid.rpc.invoke.consumer.test.User,java.lang.String => 或者更多[(,...)]
                String key = methodName + "@" + methodParameterTypes.substring(0, methodParameterTypes.length()-1);

                // eg: =>
                //	hello@java.lang.String
                //	hello@com.bfxy.rapid.rpc.invoke.consumer.test.User
                methodMap.put(key, key);
            }

            // 为当前服务持久化元数据信息
            //	key: ==>	/rapid-rpc/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.0
            //	value: ==> methodMap to json
            zookeeperClient.addPersistentNode(registryKey, FastJsonConvertUtil.convertObjectToJSON(methodMap));

            // 为当前服务持久化Providers结点
            zookeeperClient.addPersistentNode(registryKey + PROVIDERS_PATH, "");
        }

        // 为当前服务Providers结点持久化服务地址元数据信息: 值是类权重信息
        //	key: /rapid-rpc/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService:1.0.0/providers/127.0.0.1:5678
        //	value: instanceMap to json
        String address = providerConfig.getAddress();
        String registryInstanceKey = registryKey + PROVIDERS_PATH + "/" + address;
        Map<String, String> instanceMap = new HashMap<>();
        instanceMap.put("weight", providerConfig.getWeight() + "");
        zookeeperClient.addEphemeralNode(registryInstanceKey, FastJsonConvertUtil.convertObjectToJSON(instanceMap));
    }
}
