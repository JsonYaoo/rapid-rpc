package com.jsonyao.rapid.rpc.client;

import com.jsonyao.rapid.rpc.codec.RpcRequest;
import com.jsonyao.rapid.rpc.codec.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Netty实现RPC框架: Client业务处理器
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    /**
     * 缓存remotePeer: 通道连接的远端地址
     */
    private Channel channel;
    private SocketAddress remotePeer;
    public SocketAddress getRemotePeer() {
        return remotePeer;
    }

    /**
     * requestId-rpcFuture
     */
    private Map<String, RpcFuture> pendingRpcTable = new ConcurrentHashMap<>();

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

    /**
     * 服务端响应时, 客户端读取Buffer数据: 已经被自定义解码器解码成RpcResponse了
     * @param ctx
     * @param rpcResponse
     * @throws Exception
     */
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
        String requestId = rpcResponse.getRequestId();
        RpcFuture rpcFuture = pendingRpcTable.get(requestId);
        if(rpcFuture != null) {
            pendingRpcTable.remove(requestId);
            rpcFuture.done(rpcResponse);
        }
    }

    /**
     * 异步发送请求: Future模型: 可以支持Future#get方法, 通过其他线程获取返回结果
     * @return
     */
    public RpcFuture sendRequest(RpcRequest request) {
        RpcFuture rpcFuture = new RpcFuture(request);
        pendingRpcTable.put(request.getRequestId(), rpcFuture);
        channel.writeAndFlush(request);
        return rpcFuture;
    }

    /**
     * Netty提供了一种主动关闭连接发的方式: 发送一个Unpooled.EMPTY_BUFFER, 这样ChannelFutureListener的Close事件就会监听到并关闭通道
     */
    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
