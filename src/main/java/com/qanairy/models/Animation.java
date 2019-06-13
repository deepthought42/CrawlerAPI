package com.qanairy.models;

import java.util.List;

public class Animation implements PathObject, Persistable {

	private String type;
	private String key;
	private List<String> image_urls;
	
	public List<String> getImageUrls() {
		return image_urls;
	}

	public void setImageUrls(List<String> image_urls) {
		this.image_urls = image_urls;
	}

	public Animation(List<String> image_urls) {
		this.image_urls = image_urls;
		setKey(generateKey());
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
		this.type = "Animation";
	}

	@Override
	public String generateKey() {
		String key = "";
		for(String url : image_urls){
			key += url;
		}
		
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(key);
	}

}
