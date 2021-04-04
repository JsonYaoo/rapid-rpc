package com.jsonyao.rapid.rpc.tests.provider;


import com.jsonyao.rapid.rpc.tests.consumer.HelloService;
import com.jsonyao.rapid.rpc.tests.consumer.User;

public class HelloServiceImpl implements HelloService {

	@Override
	public String hello(String name) {
		System.err.println("---------服务调用-------------");
		return "hello! " + name;
	}

	@Override
	public String hello(User user) {
		return "hello! " + user.getName();
	}

}
