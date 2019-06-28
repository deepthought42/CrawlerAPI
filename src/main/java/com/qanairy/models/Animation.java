package com.qanairy.models;

import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.enums.AnimationType;

@NodeEntity
public class Animation implements Transition, Persistable {

	@GeneratedValue
    @Id
	private Long id;
	
	private String type;
	private String key;
	private List<String> image_checksums;
	private AnimationType animation_type;
	
	public Animation(){}

	/**
	 * 
	 * @param image_urls 
	 * 
	 * @pre image_urls != null
	 */
	public Animation(List<String> image_checksums, AnimationType type) {
		setType("Animation");
		setImageChecksums(image_checksums);
		setAnimationType(type);
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
		for(String url : image_checksums){
			key += url;
		}
		
		return getType()+""+org.apache.commons.codec.digest.DigestUtils.sha256Hex(key);
	}

	public List<String> getImageChecksums() {
		return image_checksums;
	}

	public void setImageChecksums(List<String> image_checksums) {
		this.image_checksums = image_checksums;
	}

	public AnimationType getAnimationType() {
		return animation_type;
	}

	public void setAnimationType(AnimationType animation_type) {
		this.animation_type = animation_type;
	}
}
