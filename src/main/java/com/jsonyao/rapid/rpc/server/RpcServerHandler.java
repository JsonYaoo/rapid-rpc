package com.jsonyao.rapid.rpc.server;

import com.jsonyao.rapid.rpc.codec.RpcRequest;
import com.jsonyao.rapid.rpc.codec.RpcResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于Netty实现RPC框架: Server业务处理器
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    /**
     * interfaceName-interfaceImplClass Bean
     */
    private Map<String, Object> handlerMap;

    /**
     * 任务线程池: 用于异步提交任务, 从而不阻塞worker线程
     */
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    /**
     * Server业务处理
     * @param ctx
     * @param rpcRequest
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                RpcResponse rpcResponse = new RpcResponse();
                rpcResponse.setRequestId(rpcRequest.getRequestId());
                try {
                    // 具体Server业务处理: 解析Request请求, 并且通过GGLIB反射调用具体的本地服务代理方法
                    Object result = handler(rpcRequest);
                    rpcResponse.setResult(result);
                } catch (Throwable t) {
                    rpcResponse.setThrowable(t);
                    log.error("rpc server handle request Throwable: " + t);
                }

                // 添加handle后置处理逻辑
                ctx.writeAndFlush(rpcResponse).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if(future.isSuccess()) {
                            // afterRpcHook
                        }
                    }
                });
            }
        });
    }

    /**
     * 具体Server业务处理: 解析Request请求, 并且通过GGLIB反射调用具体的本地服务代理方法
     *      1. 解析RpcRequest
     *      2. 从handlerMap中找到具体的接口名称所绑定的具体实现类实例
     *      3. 通过反射cglib调用具体方法: 传递相关参数, 执行相关逻辑
     *      4. 返回响应信息给调用方
     *
     * @param request
     *
     * @return
     */
    private Object handler(RpcRequest request) throws InvocationTargetException {
        // 1. 解析RpcRequest
        String className = request.getClassName();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        // 2. 从handlerMap中找到具体的接口名称所绑定的具体实现类实例
        Object serviceRef = handlerMap.get(className);
        Class<?> serviceRefClass = serviceRef.getClass();

        // 3. 通过反射cglib调用具体方法: 传递相关参数, 执行相关逻辑
        FastClass serviceFastClass = FastClass.create(serviceRefClass);
        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
        Object invokeResult = serviceFastMethod.invoke(serviceRef, parameters);

        // 4. 返回响应信息给调用方
        return invokeResult;
    }

    /**
     * 异常处理: 关闭连接
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server caught throwable: " + cause);
        ctx.close();
    }
}
