package com.jsonyao.rapid.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 基于Netty实现RPC框架: 解码器
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 数据包格式不正确(必须大于4个字节), 则直接返回
        if(in.readableBytes() < 4) {
            return;
        }

        // 添加读标志
        in.markReaderIndex();

        // 1、包头(数据包大小)
        int dataLength = in.readInt();
        if(in.readableBytes() < dataLength) {
            // 如果当前读指针后面的实际内容长度小于包头记录的长度, 则代表还没读完, 重置读指针到标志位
            in.resetReaderIndex();
            return;
        }

        // 2、包体(实际内容)
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        Object obj = Serialization.deserialize(data, genericClass);

        // 然后把反序列化内容添加到buffer中, 传播到下游handler处理 eg => RpcClientHandler
        out.add(obj);
    }
}
