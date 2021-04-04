package com.jsonyao.rapid.rpc.config.provider;

import com.jsonyao.rapid.rpc.server.RpcServer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 基于Netty实现RPC框架: 服务端启动配置类
 */
@Slf4j
public class RpcServerConfig {

    private final String host = "127.0.0.1";

    protected int port;

    private List<ProviderConfig> providerConfigs;

    private RpcServer rpcServer = null;

    public RpcServerConfig(List<ProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }

    /**
     * 创建RpcServer实例
     */
    public void exporter() {
        if(rpcServer == null) {
            try {
                rpcServer = new RpcServer(host + ":" + port);
            } catch (InterruptedException e) {
                log.error("RpcServerConfig exporter exception: " + e);
            }

            // 注册服务提供者实例到Server上
            for (ProviderConfig providerConfig : providerConfigs) {
                rpcServer.registerProcessor(providerConfig);
            }
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<ProviderConfig> getProviderConfigs() {
        return providerConfigs;
    }

    public void setProviderConfigs(List<ProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }
}
