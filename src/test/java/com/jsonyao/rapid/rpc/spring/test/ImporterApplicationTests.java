package com.jsonyao.rapid.rpc.spring.test;

import com.jsonyao.rapid.rpc.spring.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application .class})
public class ImporterApplicationTests {

	@Autowired
	private HelloService helloService;

	@Autowired
	private UserService userService;
	
    @Test
    public void contextLoads() {
    	System.err.println(helloService);
    	System.err.println(userService);
    	
    	helloService.test();
    	userService.test();
    }
}