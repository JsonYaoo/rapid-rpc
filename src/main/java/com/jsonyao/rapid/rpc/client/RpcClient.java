package com.jsonyao.rapid.rpc.client;

import com.jsonyao.rapid.rpc.client.proxy.RpcAsyncProxy;
import com.jsonyao.rapid.rpc.client.proxy.RpcProxyImpl;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于Netty实现RPC框架: 客户端
 */
public class RpcClient {

    private String serverAddress;
    private List<String> serverAddressList;
    private long timeout;
    private final Map<Class<?>, Object> syncProxyInstanceMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> asyncProxyInstanceMap = new ConcurrentHashMap<>();

    private RpcConnectManager rpcConnectManager;

    public RpcClient() {

    }

    public void initClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        this.connect();
    }

    /**
     * 	initClient: 直接返回对应的代理对象，把RpcConnectManager透传到代理对象中
     * @param <T>
     * @param serverAddress
     * @param timeout
     * @param interfaceClass
     * @return RpcProxyImpl
     */
    @SuppressWarnings("unchecked")
    public <T> T initClient(List<String> serverAddress, long timeout, Class<T> interfaceClass) {
        this.serverAddressList = serverAddress;
        this.timeout = timeout;
        this.rpcConnectManager = new RpcConnectManager();
        this.rpcConnectManager.connect(this.serverAddressList);

        // init时生成RPC同步代理对象: JDK动态代理
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RpcProxyImpl<>(rpcConnectManager, interfaceClass, timeout)
        );
    }

    public RpcClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        this.rpcConnectManager = new RpcConnectManager();
        this.connect();
    }

    private void connect() {
        rpcConnectManager.connect(serverAddress);
    }

    /**
     * 更新缓存信息, 如果存在还没有连接的服务地址, 则异步发起连接
     * @param serverAddressList
     */
    public void updateConnectedServer(CopyOnWriteArrayList<String> serverAddressList) {
        this.serverAddressList = serverAddressList;
        this.serverAddress = "";
        for (String serverAddress : serverAddressList) {
            this.serverAddress += serverAddress + ",";
        }
        this.serverAddress.substring(0, serverAddress.length()-1);

        // 更新缓存信息, 如果存在还没有连接的服务地址, 则异步发起连接
        this.rpcConnectManager.updateConnectedServer(serverAddressList);
    }

    private void stop() {
        rpcConnectManager.stop();
    }

    /**
     * 生成RPC同步代理对象: JDK动态代理
     * @param interfaceClass
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T invokeSync(Class<T> interfaceClass) {
        if(syncProxyInstanceMap.containsKey(interfaceClass)) {
            return (T) syncProxyInstanceMap.get(interfaceClass);
        }

        // 创建动态代理对象
        Object proxy = Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RpcProxyImpl<>(rpcConnectManager, interfaceClass, timeout)
        );
        syncProxyInstanceMap.put(interfaceClass, proxy);
        return (T) proxy;
    }

    /**
     * 生成RPC异步代理对象: 手动实例化代理
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public <T> RpcAsyncProxy invokeAsync(Class<T> interfaceClass) {
        if(asyncProxyInstanceMap.containsKey(interfaceClass)) {
            return (RpcAsyncProxy) asyncProxyInstanceMap.get(interfaceClass);
        }

        RpcProxyImpl<T> asyncProxyInstance = new RpcProxyImpl<>(
                rpcConnectManager,
                interfaceClass,
                timeout
        );
        asyncProxyInstanceMap.put(interfaceClass, asyncProxyInstance);
        return asyncProxyInstance;
    }
}
