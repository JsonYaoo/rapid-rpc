package com.jsonyao.rapid.rpc.spring.annotation;

import com.jsonyao.rapid.rpc.spring.register.RapidComponentScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Import(RapidComponentScanRegistrar.class)
public @interface RapidComponentScan {

    String[] value() default {};


    String[] basePackages() default {};


    Class<?>[] basePackageClasses() default {};
    
}
