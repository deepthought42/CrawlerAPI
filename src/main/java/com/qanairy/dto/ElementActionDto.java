package com.qanairy.dto;

import com.qanairy.models.Action;
import com.qanairy.models.PageElement;
import com.qanairy.models.PathObject;

/**
 * Data Transfer object that describes an object composed of both {@link PageElement} and {@link Action}
 * 
 */
public class ElementActionDto{

	private PageElementDto element;
	private ActionDto action;
	
	public ElementActionDto(PageElement elem, Action action){
		setElement(new PageElementDto(elem));
		setAction(new ActionDto(action));
	}

	public PageElementDto getElement() {
		return element;
	}

	private void setElement(PageElementDto element_dto) {
		this.element = element_dto;
	}

	public ActionDto getAction() {
		return action;
	}

	private void setAction(ActionDto action_dto) {
		this.action = action_dto;
	}
}
