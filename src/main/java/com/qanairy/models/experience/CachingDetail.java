package com.qanairy.models.experience;

import java.util.Map;

/**
 * 
 */
public class CachingDetail extends AuditDetail {

	private String url;
	private Double wasted_bytes;
	private Integer total_bytes;
	private Double cache_hit_probability;
	private Integer cache_lifetime_ms;
	private Map<String, String> debug_data;
	

	public CachingDetail(String url, double wasted_bytes, int total_bytes, double cache_hit_probability, int cache_lifetime_ms, Map<String, String> debug_data) {
		setUrl(url);
		setWastedBytes(wasted_bytes);
		setTotalBytes(total_bytes);
		setCacheHitProbability(cache_hit_probability);
		setCacheLifetimeMs(cache_lifetime_ms);
		setDebugData(debug_data);
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public Double getWastedBytes() {
		return wasted_bytes;
	}
	
	public void setWastedBytes(Double wasted_bytes) {
		this.wasted_bytes = wasted_bytes;
	}
	
	public Integer getTotalBytes() {
		return total_bytes;
	}
	
	public void setTotalBytes(Integer total_bytes) {
		this.total_bytes = total_bytes;
	}
	
	public Double getCache_hit_probability() {
		return cache_hit_probability;
	}
	
	public void setCacheHitProbability(Double cache_hit_probability) {
		this.cache_hit_probability = cache_hit_probability;
	}
	
	public Integer getCacheLifetimeMs() {
		return cache_lifetime_ms;
	}
	
	public void setCacheLifetimeMs(Integer cache_lifetime_ms) {
		this.cache_lifetime_ms = cache_lifetime_ms;
	}
	
	public Map<String, String> getDebugData() {
		return debug_data;
	}
	
	public void setDebugData(Map<String, String> debug_data) {
		this.debug_data = debug_data;
	}

}
