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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于Netty实现RPC框架: 连接管理器: 解析地址、建立连接、添加连接缓存、失败监听(清除失败连接资源、失败重连)、成功监听、释放所有连接资源、取模方式轮训选择业务处理器、关闭连接管理器服务、重新发起一次连接
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
    private long connectTimeoutMills = 6000;// 连接选择器等待的超时时间
    private volatile boolean isRunning = true;// 程序开关: 连接管理器运行状态
    private volatile AtomicInteger handlerIndex = new AtomicInteger(0);// 当前连接选择器所选的业务处理器索引 => volatile是多余的, 写不写都可以, 因为AtomicInteger的value本身就是用了volatile修饰

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
    private void connectAsync(InetSocketAddress remotePeer) {
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
    private void connect(Bootstrap b, InetSocketAddress remotePeer){
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
     * 唤醒另外一端的线程(阻塞的状态中): 告知连接选择处理器有新连接加入
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
     * 等待新连接接入
     */
    private boolean waitingAvailableHandler() throws InterruptedException {
        connectedLock.lock();
        try {
            return connectedCondition.await(this.connectTimeoutMills, TimeUnit.MILLISECONDS);
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 连接选择处理器
     * @return
     */
    public RpcClientHandler chooseHandler(){
        // 复制一份handler列表, 解决线程安全问题
        CopyOnWriteArrayList<RpcClientHandler> handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlerList.clone();
        int size = handlers.size();
        while (isRunning && size <= 0) {
            try {
                if(waitingAvailableHandler()){
                    // 再复制一份handler列表, 解决线程安全问题 => 因为都通知到了, 说明有新的handler加入了, 所以需要刷新复制的列表
                    handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlerList.clone();
                    size = handlers.size();
                }
            } catch (InterruptedException e) {
                log.error(" waiting for available node is interrupted!");
                throw new RuntimeException("no connect any servers!", e);
            }
        }

        // 防止做stop时带来的风险
        if(!isRunning) return null;

        // 取模方式轮训选择业务处理器
        return handlers.get((handlerIndex.getAndAdd(1) + size) % size);
    }

    /**
     * 关闭连接管理器服务
     */
    public void stop(){
        // 程序开关为false
        isRunning = false;

        // 关闭handler连接资源
        RpcClientHandler[] rpcClientHandlers = connectedHandlerList.toArray(new RpcClientHandler[0]);
        for (RpcClientHandler rpcClientHandler : rpcClientHandlers) {
            clearConnected((InetSocketAddress) rpcClientHandler.getRemotePeer());
        }

        // 唤醒所有正在阻塞的线程, 因为程序开关发生了变化 => 这次唤醒会使得所有线程统统选择了空的业务处理器
        // 或者可以让阻塞的线程进行等待超时, 然后抛出了异常就终止了 => 但是这样不优雅
        signalAvailableHandler();

        // 关闭资源
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    /**
     * 重新发起一次连接
     */
    public void reconnect(RpcClientHandler handler, SocketAddress remotePeer) {
        // 释放旧的资源
        if(handler != null) {
            handler.close();
            connectedHandlerList.remove(handler);
            connectedHandlerMap.remove(remotePeer);
        }

        // 重新异步地发起一次连接
        connectAsync((InetSocketAddress) remotePeer);
    }

    /**
     * 连接失败时, 及时清除资源, 清空缓存: 单独清理失败的
     */
    private void clearConnected(InetSocketAddress remotePeer) {
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
        for (RpcClientHandler rpcClientHandler : connectedHandlerList) {
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
