package com.jsonyao.rapid.rpc.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import java.net.SocketAddress;

/**
 * 基于Netty实现RPC框架: Client业务处理器
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<Object> {

    /**
     * 缓存remotePeer: 通道连接的远端地址
     */
    private Channel channel;
    private SocketAddress remotePeer;
    public SocketAddress getRemotePeer() {
        return remotePeer;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // 通道激活时才知道通道连接的远端地址
        this.remotePeer = this.channel.remoteAddress();
    }

    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

    }

    /**
     * Netty提供了一种主动关闭连接发的方式: 发送一个Unpooled.EMPTY_BUFFER, 这样ChannelFutureListener的Close事件就会监听到并关闭通道
     */
    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
