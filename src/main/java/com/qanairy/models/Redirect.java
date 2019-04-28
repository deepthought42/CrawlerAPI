package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class Redirect implements PathObject, Persistable {

	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private String start_url;
	private List<String> urls;
	private List<String> image_checksums;
	private List<String> image_urls;
	
	public Redirect(){
		setUrls(new ArrayList<String>());
		setKey(generateKey());
	}
	
	public Redirect(String start_url, List<String> urls){
		assert urls != null;
		assert !urls.isEmpty();
		assert start_url != null;
		assert !start_url.isEmpty();
		
		setStartUrl(start_url);
		setUrls(urls);
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
		return "Redirect";
	}

	@Override
	public void setType(String type) {
		//does nothing
	}

	@Override
	public String generateKey() {
		return "redirect::"+urls.toString().replace(",", "").replace(" ", "").replace("[", "").replace("]", "");
	}

	public List<String> getUrls() {
		return urls;
	}

	public void setUrls(List<String> urls) {
		this.urls = urls;
	}


	public List<String> getImageChecksums() {
		return image_checksums;
	}

	public void setImageChecksums(List<String> image_checksums) {
		this.image_checksums = image_checksums;
	}

	public List<String> getImageUrls() {
		return image_urls;
	}

	public void setImageUrls(List<String> image_urls) {
		this.image_urls = image_urls;
	}

	public Long getId() {
		return id;
	}

	public String getStartUrl() {
		return start_url;
	}

	public void setStartUrl(String start_url) {
		this.start_url = start_url;
	}

}
