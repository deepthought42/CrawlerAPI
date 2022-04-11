package com.looksee.models.journeys;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.ElementState;
import com.looksee.models.LookseeObject;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;

/**
 * A Step is the increment of work that start with a {@link PageState} contians an {@link ElementState} 
 * 	 that has an {@link Action} performed on it and results in an end {@link PageState}
 */
@NodeEntity
public class Step extends LookseeObject {
	@Relationship(type = "STARTS_WITH")
	private PageState start_page;
	
	@Relationship(type = "HAS")
	private ElementState element;
	
	private String action;
	private String action_input;
	
	@Relationship(type = "ENDS_WITH")
	private PageState end_page;
	
	public Step() {}
	
	public Step(PageState start_page,
				ElementState element,
				Action action,
				String action_input, 
				PageState end_page) {
		setStartPage(start_page);
		setElementState(element);
		setAction(action);
		setActionInput(action_input);
		setEndPage(end_page);
		setKey(generateKey());
	}
	
	public PageState getStartPage() {
		return start_page;
	}
	
	public void setStartPage(PageState page_state) {
		this.start_page = page_state;
	}
	
	public ElementState getElementState() {
		return this.element;
	}
	
	public void setElementState(ElementState element) {
		this.element = element;
	}
	
	public Action getAction() {
		return Action.create(action);
	}
	
	public void setAction(Action action) {
		this.action = action.getShortName();
	}
	
	public PageState getEndPage() {
		return this.end_page;
	}
	
	public void setEndPage(PageState page_state) {
		this.end_page = page_state;
	}

	@Override
	public String generateKey() {
		String key = "";
		if(start_page != null) {
			key += start_page.getId();
		}
		if(element != null) {
			key += element.getId();
		}
		if(end_page != null) {
			key += end_page.getId();
		}
		return "step"+key+action+action_input;
	}

	public String getActionInput() {
		return action_input;
	}

	public void setActionInput(String action_input) {
		this.action_input = action_input;
	}
}
