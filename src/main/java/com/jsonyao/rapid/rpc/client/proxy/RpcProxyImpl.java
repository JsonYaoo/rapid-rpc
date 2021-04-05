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
 * 基于Netty实现RPC框架: 客户端Consumer代理
 */
public class RpcProxyImpl<T> implements InvocationHandler, RpcAsyncProxy {

    private Class<T> clazz;
    private long timeout;
    private RpcConnectManager rpcConnectManager;

    public RpcProxyImpl(RpcConnectManager rpcConnectManager, Class<T> clazz, long timeout) {
        this.clazz = clazz;
        this.timeout = timeout;
        this.rpcConnectManager = rpcConnectManager;
    }

    /**
     * 同步阻塞式代理调用: 发送Netty请求到服务端 => 发是异步的发, 但获取是同步阻塞的获取, 所以整体来讲, 还是同步阻塞式的代理调用
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
        RpcClientHandler rpcClientHandler = rpcConnectManager.chooseHandler();

        // 3. 发送真正的客户端请求, 并获取返回结果 => 发是异步的发, 但获取是同步阻塞的获取, 所以整体来讲, 还是同步阻塞式的代理调用
        RpcFuture rpcFuture = rpcClientHandler.sendRequest(request);
        return rpcFuture.get(timeout, TimeUnit.SECONDS);
    }

    /**
     * 异步式代理调用 => 发是异步的发, 返回的是Future对象, 所以整体来讲, 是异步式的代理调用
     * @param funcName
     * @param args
     * @return
     */
    @Override
    public RpcFuture call(String funcName, Object... args) {
        // 1. 设置请求对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(this.clazz.getName());
        request.setMethodName(funcName);
        request.setParameters(args);

        // TODO 偷懒型反推方式获取方法参数类型数组 => 其实应该结合参数和方法名, 利用Class对象反射获取的
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);

        // 2. 选择一个合适的Client任务处理器 => 取模方式轮训选择业务处理器
        RpcClientHandler rpcClientHandler = rpcConnectManager.chooseHandler();

        // 3. 发送真正的客户端请求, 并获取返回结果 => 发是异步的发, 返回的是Future对象, 所以整体来讲, 是异步式的代理调用
        return rpcClientHandler.sendRequest(request);
    }

    /**
     * 根据参数获取对应的参数类型
     * @param obj
     * @return
     */
    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        if (typeName.equals("java.lang.Integer")) {
            return Integer.TYPE;
        } else if (typeName.equals("java.lang.Long")) {
            return Long.TYPE;
        } else if (typeName.equals("java.lang.Float")) {
            return Float.TYPE;
        } else if (typeName.equals("java.lang.Double")) {
            return Double.TYPE;
        } else if (typeName.equals("java.lang.Character")) {
            return Character.TYPE;
        } else if (typeName.equals("java.lang.Boolean")) {
            return Boolean.TYPE;
        } else if (typeName.equals("java.lang.Short")) {
            return Short.TYPE;
        } else if (typeName.equals("java.lang.Byte")) {
            return Byte.TYPE;
        }
        return classType;
    }
}
