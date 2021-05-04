package com.looksee.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.models.enums.FormStatus;
import com.looksee.models.enums.FormType;
import com.looksee.models.experience.BugMessage;

/**
 * Represents a form tag and the encompassed inputs in a web browser
 */
public class Form extends LookseeObject{
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Form.class);

	private Long memory_id;
	private String name;
    
	private double[] predictions;
	private Date date_discovered;
	private String status;
	private String type;
	
	@Relationship(type = "HAS")
	private List<BugMessage> bug_messages;
	
	@Relationship(type = "DEFINED_BY")
	private Element form_tag;
	
	@Relationship(type = "HAS")
	private List<Element> form_fields;
	
	@Relationship(type = "HAS_SUBMIT")
	private Element submit_field;
	
	public Form(){	}
	
	public Form(Element form_tag, List<Element> form_fields, Element submit_field, 
				String name, double[] predictions, FormType type, Date date_discovered, 
				FormStatus status){
		setFormTag(form_tag);
		setFormFields(form_fields);
		setSubmitField(submit_field);
		setType(determineFormType());
		setName(name);
		setType(type);
		setPredictions(predictions);
		setDateDiscovered(date_discovered);
		setStatus(status);
		setKey(generateKey());
	}
	
	/**
	 * Generates key for form based on element within it and the key of the form tag itself
	 * 
	 * @return
	 */
	@Override
	public String generateKey() {		
		return "form"+getFormTag();
	}

	/**
	 * Returns the {@link FormType} of the form based on attribute values on the form tag
	 * 
	 * @return {@link FormType}
	 */
	private FormType determineFormType(){
		Map<String, String> attributes = this.form_tag.getAttributes();
		for(String attr: attributes.keySet()){
			String vals = attributes.get(attr);
			if(vals.contains("register") || (vals.contains("sign") && vals.contains("up"))){
				return FormType.REGISTRATION;
			}
			else if(vals.contains("login") || (vals.contains("sign") && vals.contains("in"))){
				return FormType.LOGIN;
			}
			else if(vals.contains("search")){
				return FormType.SEARCH;
			}
			else if(vals.contains("reset") && vals.contains("password")){
				return FormType.PASSWORD_RESET;
			}
			else if(vals.contains("payment") || vals.contains("credit")){
				return FormType.PAYMENT;
			}
		}
		
		return FormType.LEAD;
	}
	
	/**
	 * Checks if {@link Form forms} are equal
	 * 
	 * @param elem
	 * @return whether or not elements are equal
	 */
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof Form)) return false;
        
        Form that = (Form)o;
		return this.getKey().equals(that.getKey());
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
	
	public List<Element> getFormFields() {
		return form_fields;
	}
	
	public boolean addFormField(Element form_field) {
		return this.form_fields.add(form_field);
	}
	
	public boolean addFormFields(List<Element> form_field) {
		return this.form_fields.addAll(form_field);
	}
	
	public void setFormFields(List<Element> form_fields) {
		this.form_fields = form_fields;
	}

	public Element getSubmitField() {
		return submit_field;
	}

	public void setSubmitField(Element submit_field) {
		this.submit_field = submit_field;
	}

	public Element getFormTag() {
		return form_tag;
	}

	public void setFormTag(Element form_tag) {
		this.form_tag = form_tag;
	}

	public FormType getType() {
		return FormType.valueOf(type.toUpperCase());
	}

	public void setType(FormType type) {
		this.type = type.toString();
	}
	
	public FormStatus getStatus() {
		return FormStatus.valueOf(status.toUpperCase());
	}

	public void setStatus(FormStatus status) {
		this.status = status.toString();
	}

	public double[] getPrediction() {
		return predictions;
	}

	public void setPredictions(double[] predictions) {
		this.predictions = predictions;
	}

	public Long getMemoryId() {
		return memory_id;
	}

	public void setMemoryId(Long memory_id) {
		this.memory_id = memory_id;
	}
	
	@Override
	public Form clone(){
		return new Form(form_tag, form_fields, submit_field, name, predictions, this.getType(), date_discovered, this.getStatus());
	}

	public List<BugMessage> getBugMessages() {
		return bug_messages;
	}

	public void setBugMessages(List<BugMessage> bug_messages) {
		if(this.bug_messages == null) {
			this.bug_messages = new ArrayList<>();
		}
		this.bug_messages = bug_messages;
	}
	
	public void addBugMessage(BugMessage bug_message) {
		if(this.bug_messages == null) {
			this.bug_messages = new ArrayList<>();
		}
		log.warn("bug meesages  :: "+this.bug_messages);
		this.bug_messages.add(bug_message);
	}

	public void removeBugMessage(BugMessage msg) {
		int idx = bug_messages.indexOf(msg);
		this.bug_messages.remove(idx);
	}
}
