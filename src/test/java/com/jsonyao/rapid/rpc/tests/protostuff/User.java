package com.jsonyao.rapid.rpc.tests.protostuff;

import lombok.Builder;
import lombok.Data;

/**
 * 测试Protostuff
 */
@Data
@Builder
public class User {
	
    private String id;

    private String name;

    private Integer age;

    private String desc;
    
}