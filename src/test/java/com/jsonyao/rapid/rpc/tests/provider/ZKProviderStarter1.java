package com.jsonyao.rapid.rpc.tests.provider;

import com.jsonyao.rapid.rpc.config.provider.ProviderConfig;
import com.jsonyao.rapid.rpc.config.provider.RpcServerConfig;
import com.jsonyao.rapid.rpc.registry.RpcRegistryProviderService;
import com.jsonyao.rapid.rpc.zookeeper.CuratorImpl;
import com.jsonyao.rapid.rpc.zookeeper.ZookeeperClient;

import java.util.ArrayList;
import java.util.List;

public class ZKProviderStarter1 {

	public static void main(String[] args) {
		
		//	服务端启动
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					ProviderConfig providerConfig = new ProviderConfig();
					providerConfig.setInterfaceName("com.jsonyao.rapid.rpc.tests.consumer.HelloService");
					HelloServiceImpl hellpHelloServiceImpl = HelloServiceImpl.class.newInstance();
					providerConfig.setRef(hellpHelloServiceImpl);
					
					List<ProviderConfig> providerConfigs = new ArrayList<ProviderConfig>();
					providerConfigs.add(providerConfig);
					
					//	添加注册中心：实例化client对象，CuratorImpl
					ZookeeperClient zookeeperClient = new CuratorImpl("127.0.0.1:2181", 10000);
					RpcRegistryProviderService registryProviderService = new RpcRegistryProviderService(zookeeperClient);
					RpcServerConfig rpcServerConfig = new RpcServerConfig(providerConfigs, registryProviderService);
					rpcServerConfig.setPort(8765);
					rpcServerConfig.exporter();
					
				} catch(Exception e){
					e.printStackTrace();
				}	
			}
		}).start();
		
	}
}
