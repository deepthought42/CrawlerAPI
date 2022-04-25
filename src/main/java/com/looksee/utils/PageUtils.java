package com.looksee.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.models.ElementState;
import com.looksee.models.Form;
import com.looksee.models.PageState;

public class PageUtils {
	private static Logger log = LoggerFactory.getLogger(PageUtils.class);

	/**
	 * Extracts all forms including the child inputs and associated labels.
	 * @param elem
	 * @param tag
	 * @param driver
	 *
	 * @return
	 * @throws Exception
	 */
	public static Set<Form> extractAllForms(PageState page) throws Exception {
		Set<Form> form_list = new HashSet<Form>();
		
		//filter all elements that aren't the main form element
		List<ElementState> forms = page.getElements()
										.parallelStream()
										.filter(element -> {
											String xpath = element.getXpath();
											int last_idx = xpath.lastIndexOf("/");
											if(last_idx+5 <= xpath.length()) {
												return element.getXpath().substring(last_idx, last_idx+5).contains("/form");
											}
											else {
												return false;
											}
										})
										.collect(Collectors.toList());
		log.warn("FORMS FOUND :: "+forms.size());
		for(ElementState form_element : forms){
			
			
			List<ElementState> form_elements = page.getElements()
													.parallelStream()
													.filter(element -> element.getXpath().contains(form_element.getXpath()))
													.filter(element -> !element.getXpath().equals(form_element.getXpath()))
													.collect(Collectors.toList());
		
			log.warn("form elements identified :: "+form_elements.size());
			List<ElementState> input_elements =  form_elements.parallelStream()
															.filter(element -> { 
																return element.getName().contentEquals("input") 
																			|| element.getName().contentEquals("button")
																			|| element.getAllText().toLowerCase().contains("sign in");
															})
															.collect(Collectors.toList());

			log.warn("form input elements :: "+input_elements);
			Form form = new Form(form_element, 
								 input_elements, 
								 FormUtils.findFormSubmitButton(form_element, form_elements),
								 "Form #"+(forms.size()+1));
			
			form_list.add(form);
		}
		return form_list;
	}
}
