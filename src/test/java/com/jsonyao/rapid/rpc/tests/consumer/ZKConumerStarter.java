package com.jsonyao.rapid.rpc.tests.consumer;

import com.jsonyao.rapid.rpc.config.consumer.ConsumerConfig;
import com.jsonyao.rapid.rpc.config.consumer.RpcClientConfig;
import com.jsonyao.rapid.rpc.registry.RpcRegistryConsumerService;
import com.jsonyao.rapid.rpc.zookeeper.CuratorImpl;
import com.jsonyao.rapid.rpc.zookeeper.ZookeeperClient;

public class ZKConumerStarter {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		ZookeeperClient zookeeperClient = new CuratorImpl("127.0.0.1:2181", 10000);
		RpcRegistryConsumerService rpcRegistryConsumerService = new RpcRegistryConsumerService(zookeeperClient);
		RpcClientConfig rpcClientConfig = new RpcClientConfig(rpcRegistryConsumerService);
		
		Thread.sleep(5000);

		// 这里是通过consumerConfig来获取代理对象
		ConsumerConfig<HelloService> consumerConfig = (ConsumerConfig<HelloService>) rpcClientConfig.getConsumer(HelloService.class, "1.0.0");
		HelloService helloService = consumerConfig.getProxyInstance();
		String result1 = helloService.hello("baihezhuo1");
		System.err.println(result1);	
		
		String result2 = helloService.hello("baihezhuo2");
		System.err.println(result2);	
		
		String result3 = helloService.hello("baihezhuo3");
		System.err.println(result3);
	}
}
