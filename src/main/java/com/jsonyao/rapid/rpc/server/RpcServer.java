package com.jsonyao.rapid.rpc.server;

import com.jsonyao.rapid.rpc.codec.RpcDecoder;
import com.jsonyao.rapid.rpc.codec.RpcEncoder;
import com.jsonyao.rapid.rpc.codec.RpcRequest;
import com.jsonyao.rapid.rpc.codec.RpcResponse;
import com.jsonyao.rapid.rpc.config.provider.ProviderConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于Netty实现RPC框架: 服务端
 */
@Slf4j
public class RpcServer {

    private String serverAddress;
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    /**
     * interfaceName-interfaceImplClass
     */
    private volatile Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer(String serverAddress) throws InterruptedException {
        this.serverAddress = serverAddress;
        this.start();
    }

    /**
     * 启动Server服务
     * @throws InterruptedException
     */
    private void start() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // tcp => sync + accept = backlog => 指的是队列长度
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 编解码的handler => Server端是对RpcRequest解码, 对RpcResponse编码
                        // 定义Netty数据包解析规则: 最大数据包大小、数据包起始位置、数据包包头长度
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
                        pipeline.addLast(new RpcDecoder(RpcRequest.class));
                        pipeline.addLast(new RpcEncoder(RpcResponse.class));

                        // 实际业务处理器rpcClientHandler
                        pipeline.addLast(new RpcServerHandler(handlerMap));
                    }
                });

        // 启动Server服务
        String[] array = serverAddress.split(":");
        String host = array[0];
        int port = Integer.parseInt(array[1]);
        ChannelFuture channelFuture = serverBootstrap.bind(host, port).sync();

        // 异步等待
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()) {
                    log.info("server success bind to " + serverAddress);
                } else {
                    log.error("server fail bind to " + serverAddress);
                    throw new RuntimeException("server start fail, cause: " + future.cause());
                }
            }
        });

        // 同步阻塞等待
        try {
            channelFuture.await(5000, TimeUnit.MILLISECONDS);
            if(channelFuture.isSuccess()) {
                log.info("start rapid rpc success!");
            }
        } catch (InterruptedException e) {
            log.error("start rapid rpc occur interrupted, ex: " + e);
        }
    }

    /**
     * 程序接口注册器
     */
    public void registerProcessor(ProviderConfig providerConfig) {
        handlerMap.put(providerConfig.getInterfaceName(), providerConfig.getRef());
    }

    /**
     * 关闭Server服务
     */
    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
