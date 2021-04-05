package com.jsonyao.rapid.rpc.spring.test;

import com.jsonyao.rapid.rpc.spring.annotation.Rapid;

@Rapid(name = "userService")
public class UserService {

	public void test() {
		System.err.println("user test...");
	}
}
