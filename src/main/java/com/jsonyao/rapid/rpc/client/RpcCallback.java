package com.jsonyao.rapid.rpc.client;

/**
 * 基于Netty实现RPC框架: 服务调用回调函数
 */
public interface RpcCallback {

    void success(Object result);

    void failure(Throwable cause);
}
