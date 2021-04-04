package com.jsonyao.rapid.rpc.tests.protostuff;

import lombok.Builder;
import lombok.Data;

/**
 * 测试Protostuff
 */
@Data
@Builder
public class Group {
	
    private String id;

    private String name;

    private User user;
    
}