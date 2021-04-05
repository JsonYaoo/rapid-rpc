package com.jsonyao.rapid.rpc.spring.register;

import com.jsonyao.rapid.rpc.spring.annotation.Rapid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 自定义Bean注册后置处理器
 */
@Slf4j
public class RapidAnnotationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware {

    private final Set<String> packagesToScan;

    private Environment environment;

    private ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    public RapidAnnotationBeanPostProcessor(String... packagesToScan) {
        this(Arrays.asList(packagesToScan));
    }
    
    public RapidAnnotationBeanPostProcessor(Collection<String> packagesToScan) {
        this(new LinkedHashSet<String>(packagesToScan));
    }
    
    public RapidAnnotationBeanPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }
    
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		Set<String> resolvedPackagesToScan = resolvePackagesToScan(packagesToScan);

        if (!CollectionUtils.isEmpty(resolvedPackagesToScan)) {
            registerServiceBeans(resolvedPackagesToScan, registry);
        } else {
            log.warn("packagesToScan is empty , ServiceBean registry will be ignored!");
        }
	}
    
	private Set<String> resolvePackagesToScan(Set<String> packagesToScan) {
        Set<String> resolvedPackagesToScan = new LinkedHashSet<String>(packagesToScan.size());
        for (String packageToScan : packagesToScan) {
            if (StringUtils.hasText(packageToScan)) {
                String resolvedPackageToScan = environment.resolvePlaceholders(packageToScan.trim());
                resolvedPackagesToScan.add(resolvedPackageToScan);
            }
        }
        return resolvedPackagesToScan;
    }

    private void registerServiceBeans(Set<String> packagesToScan, BeanDefinitionRegistry registry) {
    	
    	RapidClassPathBeanDefinitionScanner scanner =
                new RapidClassPathBeanDefinitionScanner(registry, environment, resourceLoader);

        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);

        scanner.setBeanNameGenerator(beanNameGenerator);

//        scanner.setResourceLoader(resourceLoader);
        
        scanner.addIncludeFilter(new AnnotationTypeFilter(Rapid.class));

        // 1. 首先for循环获取所有的 Rapid注解的类 --> beanDefinitionHolders
        for (String packageToScan : packagesToScan) {

            // Registers @Rapid Bean first why scan ?
            //scanner.scan(packageToScan);
            
        	Set<BeanDefinitionHolder> beanDefinitionHolders =
                    findActorBeanDefinitionHolders(scanner, packageToScan, registry, beanNameGenerator);

            if (!CollectionUtils.isEmpty(beanDefinitionHolders)) {

                for (BeanDefinitionHolder beanDefinitionHolder : beanDefinitionHolders) {
                    registerServiceBean(beanDefinitionHolder, registry, scanner);
                }

            	log.info(beanDefinitionHolders.size() + " annotated Dubbo's @Service Components { " +
                        beanDefinitionHolders +
                        " } were scanned under package[" + packageToScan + "]");

            } else {

                log.warn("No Spring Bean annotating Dubbo's @Service was found under package["
                        + packageToScan + "]");

            }

        }
    }
    
    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {

        BeanNameGenerator beanNameGenerator = null;

        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry.getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
        }

        if (beanNameGenerator == null) {

            if (log.isInfoEnabled()) {

            	log.info("BeanNameGenerator bean can't be found in BeanFactory with name ["
                        + AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR + "]");
            	log.info("BeanNameGenerator will be a instance of " +
                        AnnotationBeanNameGenerator.class.getName() +
                        " , it maybe a potential problem on bean name generation.");
            }

            beanNameGenerator = new AnnotationBeanNameGenerator();

        }

        return beanNameGenerator;

    }
    
    private Set<BeanDefinitionHolder> findActorBeanDefinitionHolders(
            ClassPathBeanDefinitionScanner scanner, String packageToScan, BeanDefinitionRegistry registry,
            BeanNameGenerator beanNameGenerator) {

        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(packageToScan);

        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<BeanDefinitionHolder>(beanDefinitions.size());

        for (BeanDefinition beanDefinition : beanDefinitions) {

            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);

        }

        return beanDefinitionHolders;

    }

    /**
     * 注册时机: 可以添加自定义内容 eg => 初始化ProviderConfig & ConsumerConfig
     * @param beanDefinitionHolder
     * @param registry
     * @param scanner
     */
	private void registerServiceBean(BeanDefinitionHolder beanDefinitionHolder, BeanDefinitionRegistry registry,
			RapidClassPathBeanDefinitionScanner scanner) {

		Class<?> beanClass = resolveClass(beanDefinitionHolder);

		Rapid rapid = AnnotationUtils.findAnnotation(beanClass, Rapid.class);

		// rapid TODO:
		@SuppressWarnings("unused")
		String name = rapid.name();
		
		String beanName = beanDefinitionHolder.getBeanName();
		
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(beanClass.getName());

		AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();

//		registry.registerBeanDefinition(beanName + "1", beanDefinition);
		registry.registerBeanDefinition(beanName, beanDefinition);

	} 
    
    private Class<?> resolveClass(BeanDefinitionHolder beanDefinitionHolder) {

        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();

        return resolveClass(beanDefinition);

    }

    private Class<?> resolveClass(BeanDefinition beanDefinition) {

        String beanClassName = beanDefinition.getBeanClassName();

        return ClassUtils.resolveClassName(beanClassName, classLoader);

    }
    
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		
	}


}
