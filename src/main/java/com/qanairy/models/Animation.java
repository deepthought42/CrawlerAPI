package com.qanairy.models;

import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class Animation implements Transition, Persistable {

	@GeneratedValue
    @Id
	private Long id;
	
	private String type;
	private String key;
	private boolean is_continuous;
	private List<String> image_urls;
	private List<String> image_checksums;
	
	public List<String> getImageUrls() {
		return image_urls;
	}

	public void setImageUrls(List<String> image_urls) {
		this.image_urls = image_urls;
	}

	/**
	 * 
	 * @param image_urls 
	 * 
	 * @pre image_urls != null
	 */
	public Animation(List<String> image_urls, boolean is_continuous, List<String> image_checksums) {
		assert image_urls != null;
		setType("Animation");
		setImageUrls(image_urls);
		setIsContinuous(is_continuous);
		setImageChecksums(image_checksums);
		setKey(generateKey());
	}

	public Long getId(){
		return this.id;
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
		
		return getType()+""+org.apache.commons.codec.digest.DigestUtils.sha256Hex(key);
	}

	public boolean getIsContinuous() {
		return is_continuous;
	}

	public void setIsContinuous(boolean is_continuous) {
		this.is_continuous = is_continuous;
	}

	public List<String> getImageChecksums() {
		return image_checksums;
	}

	public void setImageChecksums(List<String> image_checksums) {
		this.image_checksums = image_checksums;
	}

}
