package com.jsonyao.rapid.rpc.registry;

/**
 * 	AbstractRpcRegistry: 注册目录树展示:
 * 
 * 	/rapid-rpc   --->  	rapid-rpc-1.0.0
 * 		/com.bfxy.rapid.rpc.invoke.consumer.test.HelloService	=> 即AbstractRpcConfig#interfaceName
 * 			/providers
 * 				/192.168.11.101:5678
 * 				/192.168.11.102:5679
 * 			/consumers
 * 				/192.168.11.103
 * 		/com.bfxy.rapid.rpc.invoke.consumer.test.UserService	=> 即AbstractRpcConfig#interfaceName
 * 			/providers
 * 				/192.168.11.101:5678
 * 				/192.168.11.102:5679
 * 			/consumers
 * 				/192.168.11.103
 */
public abstract class AbstractRpcRegistry {
	
	protected final String ROOT_PATH = "/rapid-rpc";
	
	protected final String ROOT_VALUE = "rapid-rpc-1.0.0";
	
	protected final String PROVIDERS_PATH = "/providers";

	protected final String CONSUMERS_PATH = "/consumers";

}
