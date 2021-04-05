package com.jsonyao.rapid.rpc.config.consumer;

import com.jsonyao.rapid.rpc.client.RpcClient;
import com.jsonyao.rapid.rpc.config.AbstractRpcConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 基于Netty实现RPC框架: 服务消费者配置类
 */
public class ConsumerConfig<T> extends AbstractRpcConfig {

    /**
     * 	直连调用地址
     */
    protected volatile List<String> url;

    /**
     * 	连接超时时间
     */
    protected int connectTimeout = 3000;

    /**
     * 	代理实例对象
     */
    private volatile transient T proxyInstance;

    /**
     * Netty客户端
     */
    private RpcClient client ;

    @SuppressWarnings("unchecked")
    public void initRpcClient() {
        this.client = new RpcClient();
        this.proxyInstance = (T) this.client.initClient(url, connectTimeout, getProxyClass());
    }

    protected Class<?> getProxyClass() {
        if (proxyClass != null) {
            return proxyClass;
        }
        try {
            if (StringUtils.isNotBlank(interfaceName)) {
                this.proxyClass = Class.forName(interfaceName);
            } else {
                throw new Exception("consumer.interfaceId, null, interfaceId must be not null");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return proxyClass;
    }

    public List<String> getUrl() {
        return url;
    }

    public void setUrl(List<String> url) {
        this.url = url;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public T getProxyInstance() {
        return proxyInstance;
    }

    public void setProxyInstance(T proxyInstance) {
        this.proxyInstance = proxyInstance;
    }

    public RpcClient getClient() {
        return client;
    }

    public void setClient(RpcClient client) {
        this.client = client;
    }
}
