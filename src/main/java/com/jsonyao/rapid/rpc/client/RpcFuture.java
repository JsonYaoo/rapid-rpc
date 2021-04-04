package com.jsonyao.rapid.rpc.client;

import com.jsonyao.rapid.rpc.codec.RpcRequest;
import com.jsonyao.rapid.rpc.codec.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于Netty实现RPC框架: Client Future模型
 */
@Slf4j
public class RpcFuture implements Future<Object> {

    private final static long TIME_THRESHOLD = 5000;

    private RpcRequest request;
    private RpcResponse response;
    private long startTime;
    private Sync sync;

    private List<RpcCallback> pendingCallbacks = new ArrayList<>();
    private ReentrantLock callbackLock = new ReentrantLock();
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    public RpcFuture(RpcRequest request) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
        this.sync = new Sync();
    }

    /**
     * 可以在应用执行过程中添加回调函数
     * @return
     */
    public RpcFuture addCallback(RpcCallback rpcCallback) {
        callbackLock.lock();
        try {
            if(isDone()) {
                runCallback(rpcCallback);
            } else {
                this.pendingCallbacks.add(rpcCallback);
            }
        } finally {
            callbackLock.unlock();
        }

        return this;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    /**
     * 异步非阻塞方式执行回调处理操作
     * @param rpcResponse
     */
    public void done(RpcResponse rpcResponse) {
        this.response = response;
        boolean success = sync.release(1);
        if(success) {
            invokeCallbacks();
        }

        // 记录整个RPC调用过程的时间
        long costTime = System.currentTimeMillis() - startTime;
        if(TIME_THRESHOLD < costTime) {
            log.warn("the rpc response time is too slow, requestId = " + this.request.getRequestId() + ", cost time: " + costTime);
        }
    }

    /**
     * 执行回调函数
     */
    private void invokeCallbacks() {
        callbackLock.lock();
        try {
            for (final RpcCallback rpcCallback : pendingCallbacks) {
                runCallback(rpcCallback);
            }
        } finally {
            callbackLock.unlock();
        }
    }

    /**
     * 执行回调函数
     * @param rpcCallback
     */
    private void runCallback(RpcCallback rpcCallback) {
        final RpcResponse response = this.response;
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if(response.getThrowable() == null) {
                    rpcCallback.success(response.getResult());
                } else {
                    rpcCallback.failure(response.getThrowable());
                }
            }
        });
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    /**
     * 同步阻塞方式获取服务响应
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Override
    public Object get() throws InterruptedException, ExecutionException {
        // 同步阻塞等待
        sync.acquire(-1);
        if(this.response != null) {
            return this.response.getResult();
        } else {
            return null;
        }
    }

    /**
     * 快速失败同步阻塞方式获取服务响应
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        // 快速失败同步阻塞方式获取
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if(success) {
            if(this.response != null) {
                return this.response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("timeout exception requestId: " + this.request.getRequestId()
                                                                    + ", className: " + this.request.getClassName()
                                                                    + ", methodName: " + this.request.getMethodName());
        }
    }

    /**
     * 自定义获取许可锁, 用于done和get方法的互斥: 继承了AQS就可以拥有共享变量和同步队列
     */
    class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 3247758605222400071L;

        /**
         * 已完成: 1代表已完成, 0代表pending, -1代表已获取 => 其实随便传什么都可以, 反正底层都是AQS实现, 与传的无关
         */
        private final int done = 1;

        /**
         * 等待
         */
        private final int pending = 0;

        /**
         * 获取许可
         * @param acquires
         * @return
         */
        @Override
        protected boolean tryAcquire(int acquires) {
            return getState() == done? true : false;
        }

        /**
         * 释放许可
         * @param arg
         * @return
         */
        @Override
        protected boolean tryRelease(int arg) {
            if(getState() == pending) {
                if(compareAndSetState(pending, done)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 判断是否属于已完成状态
         * @return
         */
        public boolean isDone() {
            return getState() == done;
        }
    }
}
