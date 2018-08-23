package com.qanairy.models;

import java.util.Date;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;

@NodeEntity
public class FormRecord {
	
	@GeneratedValue
    @Id
	private Long id;

	private String key;
	private String src;
	private String screenshot_url;
	private String name;
	private double[] predictions;
	private FormType[] type_options;
	private Date date_discovered;
	private FormStatus status;
	private PageState page_state;
	private FormType form_type;
	private Form form;
	
	public FormRecord(){}
	
	public FormRecord(String src, Form form, String screenshot_url, PageState page_state, 
					  double[] predictions, FormType[] type_options, FormStatus status){
		this.setSrc(src);
		this.setForm(form);
		this.setScreenshotUrl(screenshot_url);
		this.setPageState(page_state);
		this.setPredictions(predictions);
		this.setTypeOptions(type_options);
		this.setStatus(status);
		this.setKey(generateKey(src));
	}

	public FormRecord(String src, Form form, String screenshot_url, PageState page_state, 
					  FormType form_type, String name, Date date_discovered, FormStatus status, 
					  double[] predictions, FormType[] type_options){
		this.setSrc(src);
		this.setForm(form);
		this.setScreenshotUrl(screenshot_url);
		this.setPageState(page_state);
		this.setFormType(form_type);
		this.setName(name);
		this.setDateDiscovered(date_discovered);
		this.setStatus(status);
		this.setPredictions(predictions);
		this.setTypeOptions(type_options);
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

	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = form;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDateDiscovered() {
		return date_discovered;
	}

	public void setDateDiscovered(Date date_discovered) {
		this.date_discovered = date_discovered;
	}

	public FormStatus getStatus() {
		return status;
	}

	public void setStatus(FormStatus status) {
		this.status = status;
	}

	public double[] getPrediction() {
		return predictions;
	}

	public void setPredictions(double[] predictions) {
		this.predictions = predictions;
	}

	public FormType[] getTypeOptions() {
		return type_options;
	}

	public void setTypeOptions(FormType[] type_options) {
		this.type_options = type_options;
	}
}
