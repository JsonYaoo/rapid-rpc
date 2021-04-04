package com.jsonyao.rapid.rpc.client;

import com.jsonyao.rapid.rpc.client.proxy.RpcAsyncProxy;
import com.jsonyao.rapid.rpc.client.proxy.RpcProxyImpl;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Netty实现RPC框架: 客户端
 */
public class RpcClient {

    private String serverAddress;
    private long timeout;
    private final Map<Class<?>, Object> syncProxyInstanceMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> asyncProxyInstanceMap = new ConcurrentHashMap<>();

    public RpcClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        this.connect();
    }

    private void connect() {
        RpcConnectManager.getInstance().connect(serverAddress);
    }

    private void stop() {
        RpcConnectManager.getInstance().stop();
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
        Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, new RpcProxyImpl<>(interfaceClass, timeout));
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

        RpcProxyImpl<T> asyncProxyInstance = new RpcProxyImpl<>(interfaceClass, timeout);
        asyncProxyInstanceMap.put(interfaceClass, asyncProxyInstance);
        return asyncProxyInstance;
    }
}
