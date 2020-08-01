package com.qanairy.models.audit;

import com.qanairy.models.enums.ObservationType;

/**
 * Details observations for when a page is devoid of a certain styling such as padding, 
 * that should be used, because it adds extra white-space to the content
 */
public class StylingMissingObservation implements Observation {

	private String description;
	
	public StylingMissingObservation(String description) {
		setDescription(description);
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public ObservationType getType() {
		return ObservationType.STYLE_MISSING;
	}

}
