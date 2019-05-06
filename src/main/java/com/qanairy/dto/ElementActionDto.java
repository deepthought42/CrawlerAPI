package com.qanairy.dto;

import com.qanairy.models.Action;
import com.qanairy.models.ElementState;

/**
 * Data Transfer object that describes an object composed of both {@link ElementState} and {@link Action}
 * 
 */
public class ElementActionDto{

	private ElementStateDto element;
	private ActionDto action;
	
	public ElementActionDto(ElementState elem, Action action){
		setElement(new ElementStateDto(elem));
		setAction(new ActionDto(action));
	}

	public ElementStateDto getElement() {
		return element;
	}

	private void setElement(ElementStateDto element_dto) {
		this.element = element_dto;
	}

	public ActionDto getAction() {
		return action;
	}

	private void setAction(ActionDto action_dto) {
		this.action = action_dto;
	}
}
