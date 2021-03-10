package com.qanairy.models.audit;

import com.qanairy.models.enums.ObservationType;

/**
 * Details observations for when a page is devoid of a certain styling such as padding, 
 * that should be used, because it adds extra white-space to the content
 */
public class StylingMissingObservation extends Observation {
	
	public StylingMissingObservation(String description, String why_it_matters, String ada_compliance) {
		super();
		
		assert description != null;
		
		setDescription(description);
		setType(ObservationType.STYLE_MISSING);
	}

	@Override
	public ObservationType getType() {
		return ObservationType.STYLE_MISSING;
	}

}
