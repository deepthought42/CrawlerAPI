package com.qanairy.dto;

import com.qanairy.models.Action;
import com.qanairy.models.PageElement;
import com.qanairy.models.PathObject;

/**
 * Data Transfer object that describes an object composed of both {@link PageElement} and {@link Action}
 * 
 */
public class ElementActionDto{

	private PageElementDto element_dto;
	private ActionDto action_dto;
	
	public ElementActionDto(PageElement elem, Action action){
		setElementDto(new PageElementDto(elem));
		setActionDto(new ActionDto(action));
	}

	public PageElementDto getElemenDto() {
		return element_dto;
	}

	private void setElementDto(PageElementDto element_dto) {
		this.element_dto = element_dto;
	}

	public ActionDto getActionDto() {
		return action_dto;
	}

	private void setActionDto(ActionDto action_dto) {
		this.action_dto = action_dto;
	}
}
