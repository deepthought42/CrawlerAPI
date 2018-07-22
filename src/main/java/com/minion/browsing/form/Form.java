package com.minion.browsing.form;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import com.minion.browsing.element.ComplexField;
import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.models.enums.FormType;

/**
 * Represents a form tag and the encompassed inputs in a web browser
 */
public class Form {
	private static Logger log = LoggerFactory.getLogger(Form.class);

	private PageElement form_tag;
	private List<ComplexField> form_fields;
	private PageElement submit_field;
	private FormType type;
	
	public Form(PageElement form_tag, List<ComplexField> form_fields, PageElement submit_field){
		setFormTag(form_tag);
		setFormFields(form_fields);
		setSubmitField(submit_field);
		setType(determineFormType());
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
	
	public List<ComplexField> getFormFields() {
		return form_fields;
	}
	
	public boolean addFormField(ComplexField form_field) {
		return this.form_fields.add(form_field);
	}
	
	public void setFormFields(List<ComplexField> form_fields) {
		this.form_fields = form_fields;
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
		return type;
	}

	public void setType(FormType type) {
		this.type = type;
	}
}
