package com.jsonyao.rapid.rpc.tests.consumer;

import com.jsonyao.rapid.rpc.client.RpcClient;
import com.jsonyao.rapid.rpc.client.RpcFuture;
import com.jsonyao.rapid.rpc.client.proxy.RpcAsyncProxy;

import java.util.concurrent.ExecutionException;

public class ConsumerStarter {

	public static void main(String[] args) throws Exception {
//		sync();
		async();
	}

	public static void sync() throws InterruptedException {
		//	rpcClient
		RpcClient rpcClient = new RpcClient();
		rpcClient.initClient("127.0.0.1:8765", 3000);
		HelloService helloService = rpcClient.invokeSync(HelloService.class);
		String result = helloService.hello("zhang3");
		System.err.println(result);
	}
	
	public static void async() throws InterruptedException, ExecutionException {
		RpcClient rpcClient = new RpcClient();
		rpcClient.initClient("127.0.0.1:8765", 3000);
		RpcAsyncProxy proxy = rpcClient.invokeAsync(HelloService.class);
		RpcFuture future = proxy.call("hello", "li4");
		RpcFuture future2 = proxy.call("hello", new User("001", "wang5"));

		// 异步方式调用: 把阻塞时机交给了调用者, 调用者可以组合出更多的调用结果
		Object result = future.get();
		Object result2 = future2.get();
		System.err.println("result: " + result);
		System.err.println("result2: " + result2);

	}
}
