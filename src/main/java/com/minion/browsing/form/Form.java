package com.minion.browsing.form;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.minion.browsing.element.ComplexField;
import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;

/**
 * Represents a form tag and the encompassed inputs in a web browser
 */
public class Form {
	private static Logger log = LogManager.getLogger(Form.class);

	private PageElement form_tag;
	private List<ComplexField> form_fields;
	private PageElement submit_field;
	private FormType type;
	
	/**
	 * Constructs new Form object with form_fields
	 * @param form_fields
	 */
	public Form(PageElement form_tag, List<ComplexField> form_fields){
		this.setFormTag(form_tag);
		this.form_fields = form_fields;
		this.setType(determineFormType());
	}
	
	public Form(PageElement form_tag, List<ComplexField> form_fields, PageElement submit_field){
		this.setFormTag(form_tag);
		this.form_fields = form_fields;
		this.submit_field = submit_field;
		this.setType(determineFormType());
	}
	
	/**
	 * Returns the {@link FormType} of the form based on attribute values on the form tag
	 * 
	 * @return {@link FormType}
	 */
	private FormType determineFormType(){
		List<Attribute> attributes = this.form_tag.getAttributes();
		FormType type = null;
		for(Attribute attr: attributes){
			for(String val : attr.getVals()){
				System.out.println("FORM TAG ATTRIBUTE :: "+val);
				if(val.contains("register") || (val.contains("sign") && val.contains("up"))){
					return FormType.REGISTER;
				}
				else if(val.contains("login") || (val.contains("sign") && val.contains("in"))){
					return FormType.LOGIN;
				}
				else if(val.contains("search")){
					return FormType.SEARCH;
				}
				else if(val.contains("reset") && val.contains("password")){
					return FormType.RESET_PASSWORD;
				}
				else if(val.contains("payment") || val.contains("credit")){
					return FormType.PAYMENT;
				}
			}
		}
		
		return FormType.GENERAL_RECORD;
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
