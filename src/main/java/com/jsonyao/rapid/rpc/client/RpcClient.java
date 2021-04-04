package com.jsonyao.rapid.rpc.client;

/**
 * 基于Netty实现RPC框架: 客户端
 */
public class RpcClient {

    private String serverAddress;
    private long timeout;

    public RpcClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        this.connect();
    }

    private void connect() {
        RpcConnectManager.getInstance().connect(serverAddress);
    }

    private void stop() {
        RpcConnectManager.getInstance().stop();
    }
}
