package com.jsonyao.rapid.rpc.config.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 基于Netty实现RPC框架: 客户端服务发现后设置的服务缓存
 */
@Data
@AllArgsConstructor
public class CachedService {

	private String address;
	private int weight;

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		CachedService other = (CachedService) obj;
		if (address == null) { if (other.address != null) return false; }
		else if (!address.equals(other.address)) return false;
		if (weight != other.weight) return false;

		// 只有address和weight都相同时才返回true
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + weight;
		return result;
	}
}
