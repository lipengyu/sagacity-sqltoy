/**
 * 
 */
package org.sagacity.sqltoy.translate.cache.impl;

import java.time.Duration;
import java.util.HashMap;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.sagacity.sqltoy.translate.cache.TranslateCacheManager;
import org.sagacity.sqltoy.translate.model.TranslateConfigModel;
import org.sagacity.sqltoy.utils.StringUtil;

/**
 * @project sagacity-sqltoy4.0
 * @description 基于ehcache缓存实现translate 提取缓存数据和存放缓存
 * @author zhongxu <a href="mailto:zhongxuchen@gmail.com">联系作者</a>
 * @version id:TranslateEhcacheManager.java,Revision:v1.0,Date:2013-4-14
 */
@SuppressWarnings("unchecked")
public class TranslateEhcacheManager extends TranslateCacheManager {

	/**
	 * 缓存大小超出范围后存储磁盘路径
	 */
	private String diskStorePath = "./translateCaches";

	/**
	 * @param diskStorePath
	 *            the diskStorePath to set
	 */
	public void setDiskStorePath(String diskStorePath) {
		this.diskStorePath = diskStorePath;
	}

	private static CacheManager cacheManager;

	@Override
	public HashMap<String, Object[]> getCache(String cacheName, String cacheKey) {
		Cache<String, HashMap> cache = cacheManager.getCache(cacheName, String.class, HashMap.class);
		if (cache == null)
			return null;
		Object cacheValue = cache.get(StringUtil.isNotBlank(cacheKey) ? cacheKey : cacheName);
		if (cacheValue != null)
			return (HashMap<String, Object[]>) cacheValue;
		return null;
	}

	@Override
	public void put(TranslateConfigModel cacheConfig, String cacheName, String cacheKey,
			HashMap<String, Object[]> cacheValue) {
		synchronized (cacheName) {
			Cache<String, HashMap> cache = cacheManager.getCache(cacheName, String.class, HashMap.class);
			// 缓存没有配置,自动创建缓存不建议使用
			if (cache == null) {
				int heap = StringUtil.isBlank(cacheKey) ? 1 : cacheConfig.getHeap();
				cache = cacheManager.createCache(cacheName,
						CacheConfigurationBuilder
								.newCacheConfigurationBuilder(String.class, HashMap.class,
										ResourcePoolsBuilder.heap(heap).offheap(cacheConfig.getOffHeap(), MemoryUnit.MB)
												.disk(cacheConfig.getDiskSize(), MemoryUnit.MB, true))
								.withExpiry(ExpiryPolicyBuilder
										.timeToLiveExpiration(Duration.ofSeconds(cacheConfig.getKeepAlive())))
								.build());
			}
			// 清除缓存
			if (cacheValue == null) {
				if (StringUtil.isBlank(cacheKey))
					cache.clear();
				else
					cache.remove(cacheKey);
			}
			// 更新缓存
			else
				cache.put(StringUtil.isBlank(cacheKey) ? cacheName : cacheKey, cacheValue);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagacity.sqltoy.translate.cache.TranslateCacheManager#clear(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	public void clear(String cacheName, String cacheKey) {
		synchronized (cacheName) {
			Cache<String, HashMap> cache = cacheManager.getCache(cacheName, String.class, HashMap.class);
			// 缓存没有配置,自动创建缓存不建议使用
			if (cache != null) {
				if (StringUtil.isBlank(cacheKey))
					cache.clear();
				else
					cache.remove(cacheKey);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagacity.sqltoy.translate.cache.TranslateCacheManager#init()
	 */
	@Override
	public void init() {
		if (cacheManager == null) {
			if (StringUtil.isBlank(diskStorePath))
				cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
			else
				cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
						.with(CacheManagerBuilder.persistence(diskStorePath)).build(true);
		}
	}
}
