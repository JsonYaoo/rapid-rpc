package com.jsonyao.rapid.rpc.config.provider;

import com.jsonyao.rapid.rpc.config.AbstractRpcConfig;

/**
 * 基于Netty实现RPC框架: 服务提供者配置类: 接口名称 & 引用逻辑程序对象 => 即元数据信息
 */
public class ProviderConfig extends AbstractRpcConfig {

    protected Object ref;

    protected String address;// ip:port => 服务地址

    protected String version = "1.0.0";

    protected int weight = 1;// 权重

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
