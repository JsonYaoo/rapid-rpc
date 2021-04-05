package com.jsonyao.rapid.rpc.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Rapid {
	
	String name();
	
}
