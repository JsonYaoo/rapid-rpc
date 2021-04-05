package com.jsonyao.rapid.rpc.spring.register;

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * 	$RapidClassPathBeanDefinitionScanner
 * 
 * 
 * 	PS: 	
 * 	我们并不知道需要register哪些bean，这里我们还需要借助一个类ClassPathBeanDefinitionScanner，也就是扫描器；
 * 	通过扫描器获取我们需要注册的bean。先简单看一下spring源码中的的定义：
 * 
 * 	需要继承ClassPathBeanDefinitionScanner，扫描使用-->@Actor的注解的类，
 * 	ClassPathBeanDefinitionScanner又继承 ClassPathScanningCandidateComponentProvider类，
 * 	
 * 	ClassPathScanningCandidateComponentProvider中有两个TypeFilter集合：
 * 	includeFilters
 * 	excludeFilters
 * 	满足任意includeFilters会被加载，同样的满足任意excludeFilters不会被加载	~
 * 
 * @author hezhuo.bai-JiFeng
 * @since 2019年5月13日 下午3:45:58
 */
public class RapidClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

	public RapidClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment environment, ResourceLoader resourceLoader) {
		super(registry, useDefaultFilters);
        setEnvironment(environment);
        setResourceLoader(resourceLoader);
        AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);
	}
	
    public RapidClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, Environment environment, ResourceLoader resourceLoader) {
    	this(registry, false, environment, resourceLoader);
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        return super.doScan(basePackages);
    }
    
    @Override
    public boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        return super.checkCandidate(beanName, beanDefinition);
    }
    
}
