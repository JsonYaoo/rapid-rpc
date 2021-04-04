package com.jsonyao.rapid.rpc.client.proxy;

import com.jsonyao.rapid.rpc.client.RpcClientHandler;
import com.jsonyao.rapid.rpc.client.RpcConnectManager;
import com.jsonyao.rapid.rpc.client.RpcFuture;
import com.jsonyao.rapid.rpc.codec.RpcRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于Netty实现RPC框架: 客户端Consumer动态代理类
 */
public class RpcProxyImpl<T> implements InvocationHandler {

    private Class<T> clazz;
    private long timeout;

    public RpcProxyImpl(Class<T> clazz, long timeout) {
        this.clazz = clazz;
        this.timeout = timeout;
    }

    /**
     * 具体动态代理逻辑: 发送Netty请求到服务端 => 发是异步的发, 但获取是同步阻塞的获取, 所以整体来讲, 还是同步阻塞式的代理调用
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 设置请求对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);

        // 2. 选择一个合适的Client任务处理器 => 取模方式轮训选择业务处理器
        RpcClientHandler rpcClientHandler = RpcConnectManager.getInstance().chooseHandler();

        // 3. 发送真正的客户端请求, 并获取返回结果 => 发是异步的发, 但获取是同步阻塞的获取, 所以整体来讲, 还是同步阻塞式的代理调用
        RpcFuture rpcFuture = rpcClientHandler.sendRequest(request);
        return rpcFuture.get(timeout, TimeUnit.SECONDS);
    }
}
