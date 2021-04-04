package com.jsonyao.rapid.rpc.tests.consumer;

public interface HelloService {

    String hello(String name);

    String hello(User user);

}
