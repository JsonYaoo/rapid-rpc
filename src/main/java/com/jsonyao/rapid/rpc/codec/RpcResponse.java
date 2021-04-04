package com.jsonyao.rapid.rpc.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * 基于Netty实现RPC框架: RpcResponse
 */
@Data
public class RpcResponse implements Serializable {

    private static final long serialVersionUID = 915558663286057210L;

    private String requestId;

    private Object result;

    private Throwable throwable;

}
