package com.jsonyao.rapid.rpc.server;

import com.jsonyao.rapid.rpc.codec.RpcRequest;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.Map;

/**
 * 基于Netty实现RPC框架: Server业务处理器
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    /**
     * interfaceName-interfaceImplClass
     */
    private Map<String, Object> handlerMap;

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        // 1. 解析RpcRequest
        // 2. 从handlerMap中找到具体的接口名称所绑定的具体实现类实例
        // 3. 通过反射cglib调用具体方法: 传递相关参数, 执行相关逻辑
        // 4. 返回响应信息给调用方
    }
}
