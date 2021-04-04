package com.jsonyao.rapid.rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于Netty实现RPC框架: 连接管理器: 解析地址、建立连接、添加连接缓存、失败监听(清除失败连接资源、失败重连)、成功监听、释放所有连接资源
 */
@Slf4j
public class RpcConnectManager {

    /**
     * 单例模式: 饥饿式
     */
    private static volatile RpcConnectManager RPC_CONNECT_MANAGER = new RpcConnectManager();
    public static RpcConnectManager getInstance() {
        return RPC_CONNECT_MANAGER;
    }

    // 私有构造函数 => 由于是单例
    private RpcConnectManager() {

    }

    /**
     * InetSocketAddress-RpcClientHandler
     */
    private Map<InetSocketAddress, RpcClientHandler> connectedHandlerMap = new ConcurrentHashMap<InetSocketAddress, RpcClientHandler>();

    /**
     * 已连接成功的RpcClientHandler
     */
    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlerList = new CopyOnWriteArrayList<RpcClientHandler>();

    /**
     * 用于发起异步连接的线程池
     */
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    /**
     * 连接线程组
     */
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    /**
     * connectedCondition#signalAll唤醒另外一端的线程(阻塞的状态中): 告知有新连接加入
     */
    private ReentrantLock connectedLock = new ReentrantLock();
    private Condition connectedCondition = connectedLock.newCondition();

    // 1. 异步连接、线程池、真正发起连接、连接失败监听、连接成功监听
    // 2. 对于连接进来的资源做一个缓存(即管理)
    /**
     * 发起连接
     * @param serverAddress
     */
    public void connect(final String serverAddress) {
        List<String> allServerAddress = Arrays.asList(serverAddress.split(","));
        updateConnectedServer(allServerAddress);
    }

    /**
     * 更新缓存信息, 并异步发起连接
     */
    public void updateConnectedServer(List<String> allServerAddress) {
        if(CollectionUtils.isEmpty(allServerAddress)) {
            log.error(" no available server address!");
            // 清除所有缓存
            clearAllConnected();
            return;
        }

        // 192.168.1.100:8765,192.168.1.101:8765
        // 1. 解析allServerAddress地址, 并且临时存储到HashSet中 => 注意, 这里InetSocketAddress已经重写了equals方法, 可以达到去重的效果
        Set<InetSocketAddress> newAllServerNodeSet = new HashSet<InetSocketAddress>();
        for (String serverAddress : allServerAddress) {
            String[] array = serverAddress.split(":");
            if(array.length == 2) {
                String host = array[0];
                int port = Integer.parseInt(array[1]);
                InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                newAllServerNodeSet.add(inetSocketAddress);
            }
        }

        // 2. 调用建立连接方法, 发起远程连接操作
        Set<InetSocketAddress> existedInetSocketAddresses = connectedHandlerMap.keySet();
        for (InetSocketAddress inetSocketAddress : newAllServerNodeSet) {
            if(existedInetSocketAddresses.contains(inetSocketAddress)){
                connectAsync(inetSocketAddress);
            }
        }

        // 3. 如果allServerAddress没出现对应的地址, 则需要从缓存中移除
        RpcClientHandler[] rpcClientHandlers = connectedHandlerList.toArray(new RpcClientHandler[0]);
        for (int i = 0; i < rpcClientHandlers.length; i++) {
            RpcClientHandler rpcClientHandler = rpcClientHandlers[i];
            SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
            if(newAllServerNodeSet.contains(remotePeer)){
                log.info(" remove invalid server node " + remotePeer);
                clearConnected((InetSocketAddress) remotePeer);
            }
        }
    }

    /**
     * 异步发起连接
     * @param remotePeer
     */
    private void connectAsync(final InetSocketAddress remotePeer) {
        threadPoolExecutor.submit(new Runnable() {
            public void run() {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap
                        .group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new RpcClientInitializer());
                connect(bootstrap, remotePeer);
            }
        });
    }

    /**
     * 发起连接
     * @param b
     * @param remotePeer
     */
    private void connect(final Bootstrap b, final InetSocketAddress remotePeer){
        // 1. 真正的建立连接
        final ChannelFuture channelFuture = b.connect(remotePeer);

        // 2. 连接失败的时候添加监听: 用于清除连接失败的资源, 发起重连操作
        channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("channelFuture.channel close operationComplete, remote peer = " + remotePeer);
                future.channel().eventLoop().schedule(new Runnable() {
                    public void run() {
                        log.warn(" connect fail, to reconnect!");
                        clearConnected(remotePeer);
                        connect(b, remotePeer);
                    }
                }, 3, TimeUnit.SECONDS);
            }
        });

        // 3. 连接成功的时候添加监听: 用于把新的连接放入缓存中
        channelFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    log.info("successfully connect to remote server, remote peer = " + remotePeer);
                    RpcClientHandler handler = future.channel().pipeline().get(RpcClientHandler.class);
                    addHandler(handler);
                }
            }
        });
    }

    /**
     * 添加RpcClientHandler到缓存中: connectedHandlerList & connectedHandlerMap
     * @param handler
     */
    private void addHandler(RpcClientHandler handler) {
        connectedHandlerList.add(handler);
        connectedHandlerMap.put((InetSocketAddress) handler.getRemotePeer(), handler);

        // 唤醒可用的业务执行器 signalAvailableHandler TODO
        signalAvailableHandler();
    }

    /**
     * 唤醒另外一端的线程(阻塞的状态中): 告知有新连接加入
     */
    private void signalAvailableHandler() {
        connectedLock.lock();
        try {
            connectedCondition.signalAll();
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 连接失败时, 及时清除资源, 清空缓存: 单独清理失败的
     */
    private void clearConnected(final InetSocketAddress remotePeer) {
        // 然后从connectedHandlerMap缓存中移除指定的rpcClientHandler
        RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
        if(handler != null) {
            handler.close();
            connectedHandlerMap.remove(remotePeer);
        }
        connectedHandlerList.remove(handler);
    }

    /**
     * 清除所有缓存
     */
    private void clearAllConnected() {
        for (final RpcClientHandler rpcClientHandler : connectedHandlerList) {
            // 通过rpcClientHandler找到remotePeer => 在rpcClientHandler缓存了remotePeer的引用
            SocketAddress remotePeer = rpcClientHandler.getRemotePeer();

            // 然后从connectedHandlerMap缓存中移除指定的rpcClientHandler
            RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
            if(handler != null) {
                handler.close();
                connectedHandlerMap.remove(remotePeer);
            }
        }
        connectedHandlerList.clear();
    }
}
