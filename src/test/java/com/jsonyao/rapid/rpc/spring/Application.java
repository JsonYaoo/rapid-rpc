package com.jsonyao.rapid.rpc.spring;

import com.jsonyao.rapid.rpc.spring.annotation.RapidComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@Configuration
@RapidComponentScan(basePackages= {"com.jsonyao.rapid.rpc.spring"})
@SpringBootApplication
public class Application {
	
	public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.run(args);
	}
}