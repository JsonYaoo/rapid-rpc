package com.jsonyao.rapid.rpc.spring.test;

import com.jsonyao.rapid.rpc.spring.annotation.Rapid;

@Rapid(name = "helloService")
public class HelloService {

	public void test() {
		System.err.println("hello test...");
	}
}
