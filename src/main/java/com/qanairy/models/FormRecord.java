package com.qanairy.models;

import java.util.Set;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.enums.FormType;

@NodeEntity
public class FormRecord {
	
	@GeneratedValue
    @Id
	private Long id;

	private String key;
	private String src;
	private Set<PageElement> elements;
	private String screenshot_url;
	private PageState page_state;
	private FormType form_type;
	
	public FormRecord(){}
	
	public FormRecord(String src, Set<PageElement> elements, String screenshot_url, PageState page_state){
		this.setSrc(src);
		this.setElements(elements);
		this.setScreenshotUrl(screenshot_url);
		this.setPageState(page_state);
	}
	
	public FormRecord(String src, Set<PageElement> elements, String screenshot_url, PageState page_state, FormType form_type){
		this.setSrc(src);
		this.setElements(elements);
		this.setScreenshotUrl(screenshot_url);
		this.setPageState(page_state);
		this.setFormType(form_type);
		this.setKey(generateKey(src));
	}

	private String generateKey(String src) {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(src); 
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public Set<PageElement> getElements() {
		return elements;
	}

	public void setElements(Set<PageElement> elements) {
		this.elements = elements;
	}

	public String getScreenshotUrl() {
		return screenshot_url;
	}

	public void setScreenshotUrl(String screenshot_url) {
		this.screenshot_url = screenshot_url;
	}

	public PageState getPageState() {
		return page_state;
	}

	public void setPageState(PageState page_state) {
		this.page_state = page_state;
	}

	public FormType getFormType() {
		return form_type;
	}

	public void setFormType(FormType form_type) {
		this.form_type = form_type;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
