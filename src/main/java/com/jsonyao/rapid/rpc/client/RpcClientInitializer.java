package com.jsonyao.rapid.rpc.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * 基于Netty实现RPC框架: 自定义Channel初始化器
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {

    protected void initChannel(SocketChannel ch) throws Exception {
        // 编解码的handler
        // 实际业务处理器rpcClientHandler
    }
}
