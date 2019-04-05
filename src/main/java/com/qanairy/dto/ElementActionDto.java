package com.qanairy.dto;

import com.qanairy.models.Action;
import com.qanairy.models.PageElementState;

/**
 * Data Transfer object that describes an object composed of both {@link PageElementState} and {@link Action}
 * 
 */
public class ElementActionDto{

	private PageElementStateDto element;
	private ActionDto action;
	
	public ElementActionDto(PageElementState elem, Action action){
		setElement(new PageElementStateDto(elem));
		setAction(new ActionDto(action));
	}

	public PageElementStateDto getElement() {
		return element;
	}

	private void setElement(PageElementStateDto element_dto) {
		this.element = element_dto;
	}

	public ActionDto getAction() {
		return action;
	}

	private void setAction(ActionDto action_dto) {
		this.action = action_dto;
	}
}
