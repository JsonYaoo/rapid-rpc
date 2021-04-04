package com.jsonyao.rapid.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 基于Netty实现RPC框架: 编码器
 */
public class RpcEncoder extends MessageToByteEncoder<Object> {

    private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * 编码:
     *      1. 把对应的Java对象进行编码
     *      2. 之后把内容填充到Buffer中
     *      3. 然后写出到Net中的另外一端
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if(genericClass.isInstance(msg)) {
            byte[] data = Serialization.serialize(msg);

            // 然后写出到Net中的另外一端: 1、包头(数据长度) 2、包体(数据包内容)
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}
