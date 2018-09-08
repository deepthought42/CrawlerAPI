package com.qanairy.models;

import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.enums.FormType;

/**
 * Represents a form tag and the encompassed inputs in a web browser
 */
@NodeEntity
public class Form {
	
	@GeneratedValue
    @Id
	private Long id;
	
	private static Logger log = LoggerFactory.getLogger(Form.class);

	private String key;
	private PageElement form_tag;
	private List<PageElement> form_fields;
	private PageElement submit_field;
	private String type;
	
	public Form(){}
	
	public Form(PageElement form_tag, List<PageElement> form_fields, PageElement submit_field){
		setFormTag(form_tag);
		setFormFields(form_fields);
		setSubmitField(submit_field);
		setType(determineFormType());
		setKey(generateKey());
	}
	
	private String generateKey() {
		return ""+getFormFields().hashCode();
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
}
