package com.jsonyao.rapid.rpc.config;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于Netty实现RPC框架: 通用抽象配置类
 */
public abstract class AbstractRpcConfig {

    private AtomicInteger generator = new AtomicInteger(0);

    protected String id;

    protected String interfaceName = null;

    // 服务的调用方Consumer特有的属性
    protected Class<?> proxyClass = null;

    public String getId() {
        if(StringUtils.isBlank(id)) {
            id = "rapid-cfg-gen-" + generator.getAndIncrement();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
}
