package com.qanairy.models.journeys;

import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.Action;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;

/**
 * A set of Steps
 */
@Component
public class ElementInteractionStep extends Step {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementInteractionStep.class);

	@Relationship(type = "STARTS")
	private PageState start_page;
	
	@Relationship(type = "HAS")
	private ElementState element;
	
	@Relationship(type = "HAS")
	private Action action;
	
	@Relationship(type = "ENDS")
	private PageState end_page;
	
	public ElementInteractionStep() {}
	
	public ElementInteractionStep(PageState start_page, ElementState element, Action action, PageState end_page) {
		assert start_page != null;
		assert element != null;
		assert action != null;
		assert end_page != null;
		
		setStartPage(start_page);
		setElement(element);
		setAction(action);
		setEndPage(end_page);
		setKey(generateKey());
	}
	
	@Override
	public String generateKey() {
		return "elementinteractionstep:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(start_page.getKey() + element.getKey() + action.getKey() + end_page.getKey());
	}

	public PageState getStartPage() {
		return start_page;
	}

	public void setStartPage(PageState start_page) {
		this.start_page = start_page;
	}

	public ElementState getElement() {
		return element;
	}

	public void setElement(ElementState element) {
		this.element = element;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public PageState getEndPage() {
		return end_page;
	}

	public void setEndPage(PageState end_page) {
		this.end_page = end_page;
	}
}
