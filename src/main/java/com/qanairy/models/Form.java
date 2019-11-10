package com.qanairy.models;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.message.BugMessage;

/**
 * Represents a form tag and the encompassed inputs in a web browser
 */
@NodeEntity
public class Form {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Form.class);

	@GeneratedValue
    @Id
	private Long id;

	private String key;
	private Long memory_id;
	private String name;
    
	private double[] predictions;
	private Date date_discovered;
	private String status;
	private String type;
	private List<BugMessage> bug_messages;
	
	@Relationship(type = "DEFINED_BY")
	private ElementState form_tag;
	
	@Relationship(type = "HAS")
	private List<ElementState> form_fields;
	
	@Relationship(type = "HAS_SUBMIT")
	private ElementState submit_field;
	
	public Form(){}
	
	public Form(ElementState form_tag, List<ElementState> form_fields, ElementState submit_field, 
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
	private String generateKey() {
		String elements_key = "";
		List<ElementState> elements = getFormFields().stream().collect(Collectors.toList());
		Collections.sort(elements, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
		for(ElementState element : elements){
			elements_key += element.getKey();
		}
		
		return "form::"+elements_key+""+getFormTag().getKey();
	}

	/**
	 * Returns the {@link FormType} of the form based on attribute values on the form tag
	 * 
	 * @return {@link FormType}
	 */
	private FormType determineFormType(){
		Set<Attribute> attributes = this.form_tag.getAttributes();
		for(Attribute attr: attributes){
			for(String val : attr.getVals()){
				if(val.contains("register") || (val.contains("sign") && val.contains("up"))){
					return FormType.REGISTRATION;
				}
				else if(val.contains("login") || (val.contains("sign") && val.contains("in"))){
					return FormType.LOGIN;
				}
				else if(val.contains("search")){
					return FormType.SEARCH;
				}
				else if(val.contains("reset") && val.contains("password")){
					return FormType.PASSWORD_RESET;
				}
				else if(val.contains("payment") || val.contains("credit")){
					return FormType.PAYMENT;
				}
			}
		}
		
		return FormType.LEAD;
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
	public List<ElementState> getFormFields() {
		return form_fields;
	}
	
	public boolean addFormField(ElementState form_field) {
		return this.form_fields.add(form_field);
	}
	
	public boolean addFormFields(List<ElementState> form_field) {
		return this.form_fields.addAll(form_field);
	}
	
	public void setFormFields(List<ElementState> form_fields2) {
		this.form_fields = form_fields2;
	}

	public ElementState getSubmitField() {
		return submit_field;
	}

	public void setSubmitField(ElementState submit_field) {
		this.submit_field = submit_field;
	}

	public ElementState getFormTag() {
		return form_tag;
	}

	public void setFormTag(ElementState form_tag) {
		this.form_tag = form_tag;
	}

	public FormType getType() {
		return FormType.valueOf(type.toUpperCase());
	}

	public void setType(FormType type) {
		this.type = type.toString();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
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
	
	public Long getId() {
		return id;
	}
	
	@Override
	public Form clone(){
		return new Form(form_tag, form_fields, submit_field, name, predictions, this.getType(), date_discovered, this.getStatus());
	}

	public List<BugMessage> getBugMessages() {
		return bug_messages;
	}

	public void setBugMessages(List<BugMessage> bug_messages) {
		this.bug_messages = bug_messages;
	}
}
