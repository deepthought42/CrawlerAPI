package com.qanairy.models;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;

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
	private String[] type_options;
	private Date date_discovered;
	private String status;	

	private PageElement form_tag;
	private List<PageElement> form_fields;
	private PageElement submit_field;
	private String type;
	
	public Form(){}
	
	public Form(PageElement form_tag, List<PageElement> form_fields, PageElement submit_field, 
				String name, double[] predictions, FormType[] type_options, FormType type, Date date_discovered, 
				FormStatus status){
		setFormTag(form_tag);
		setFormFields(form_fields);
		setSubmitField(submit_field);
		setType(determineFormType());
		setName(name);
		setPredictions(predictions);
		setTypeOptions(type_options);
		setDateDiscovered(date_discovered);
		setStatus(status);
		setKey(generateKey());
	}
	
	private String generateKey() {
		String elements_key = "";
		for(PageElement elem : getFormFields()){
			elements_key += elem.getKey();
		}
		return "form::"+elements_key+""+getFormTag().getKey()+""+getSubmitField().getKey();
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
	public List<PageElement> getFormFields() {
		return form_fields;
	}
	
	public boolean addFormField(PageElement form_field) {
		return this.form_fields.add(form_field);
	}
	
	public boolean addFormFields(List<PageElement> form_field) {
		return this.form_fields.addAll(form_field);
	}
	
	public void setFormFields(List<PageElement> form_fields2) {
		this.form_fields = form_fields2;
	}

	public PageElement getSubmitField() {
		return submit_field;
	}

	public void setSubmitField(PageElement submit_field) {
		this.submit_field = submit_field;
	}

	public PageElement getFormTag() {
		return form_tag;
	}

	public void setFormTag(PageElement form_tag) {
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

	public FormType[] getTypeOptions() {
		FormType[] type_options = new FormType[this.type_options.length];
		for(int idx=0; idx<this.type_options.length; idx++){
			type_options[idx] = FormType.valueOf(this.type_options[idx].toUpperCase());
		}
		return type_options;
	}

	public void setTypeOptions(FormType[] type_options) {
	    this.type_options = Arrays.stream(type_options).map(Enum::name).toArray(String[]::new);
	}

	public Long getMemoryId() {
		return memory_id;
	}

	public void setMemoryId(Long memory_id) {
		this.memory_id = memory_id;
	}
}
