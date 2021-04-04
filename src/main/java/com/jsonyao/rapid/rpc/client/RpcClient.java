package com.jsonyao.rapid.rpc.client;

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
     * 同步生成RPC代理对象
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public <T> T invokeSync(Class<T> interfaceClass) {
        if(syncProxyInstanceMap.containsKey(interfaceClass)) {
            return (T) syncProxyInstanceMap.get(interfaceClass);
        }

        // 创建动态代理对象
        Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, new RpcProxyImpl<>(interfaceClass, timeout));
        syncProxyInstanceMap.put(interfaceClass, proxy);
        return (T) proxy;
    }
}
