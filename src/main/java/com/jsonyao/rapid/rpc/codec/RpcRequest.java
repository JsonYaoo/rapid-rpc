package com.jsonyao.rapid.rpc.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * 基于Netty实现RPC框架: RpcRequest
 */
@Data
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = -2392660916221950623L;

    // 用于异步请求Future模型
    private String requestId;

    private String className;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] parameters;

}
