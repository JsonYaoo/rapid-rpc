package com.jsonyao.rapid.rpc.config.provider;

import com.jsonyao.rapid.rpc.config.AbstractRpcConfig;

/**
 * 基于Netty实现RPC框架: 服务提供者配置类: 接口名称 & 引用逻辑程序对象
 */
public class ProviderConfig extends AbstractRpcConfig {

    protected Object ref;

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
