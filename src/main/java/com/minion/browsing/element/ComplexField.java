package com.minion.browsing.element;

import java.util.List;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import com.minion.browsing.form.FormField;

/**
 * Represents a container with an input field as well as label
 */
public class ComplexField {
	private static Logger log = LoggerFactory.getLogger(ComplexField.class);

	private List<FormField> elements;
	
	/**
	 * Constructs new InputContiner without a label
	 * 
	 * @param elements
	 * 
	 * @pre elements != null
	 * @pre elements.size() > 0;
	 */
	public ComplexField(List<FormField> fields){
		assert fields != null;
		assert fields.size() > 0;
		
		this.setElements(fields);
	}

	public List<FormField> getElements() {
		return elements;
	}

	public void setElements(List<FormField> elements) {
		this.elements = elements;
	}
}
