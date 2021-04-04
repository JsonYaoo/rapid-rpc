package com.jsonyao.rapid.rpc.client;

import com.jsonyao.rapid.rpc.codec.RpcDecoder;
import com.jsonyao.rapid.rpc.codec.RpcEncoder;
import com.jsonyao.rapid.rpc.codec.RpcRequest;
import com.jsonyao.rapid.rpc.codec.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 基于Netty实现RPC框架: 自定义Channel初始化器
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {

    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 编解码的handler => Client端是对RpcRequest编码, 对RpcResponse解码
        pipeline.addLast(new RpcEncoder(RpcRequest.class));
        // 定义Netty数据包解析规则: 最大数据包大小、数据包起始位置、数据包包头长度
        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        pipeline.addLast(new RpcDecoder(RpcResponse.class));

        // 实际业务处理器rpcClientHandler
        pipeline.addLast(new RpcClientHandler());
    }
}
