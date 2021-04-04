package com.jsonyao.rapid.rpc.client.proxy;

import com.jsonyao.rapid.rpc.client.RpcFuture;

/**
 * 基于Netty实现RPC框架: 客户端Consumer异步式代理接口
 */
public interface RpcAsyncProxy {

    RpcFuture call(String funcName, Object... args);

}
