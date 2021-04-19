package com.qanairy.models.audit;

import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

/**
 * Details issues for when a page is devoid of a certain styling such as padding, 
 * that should be used, because it adds extra white-space to the content
 */
public class StylingMissingIssueMessage extends UXIssueMessage {
	
	public StylingMissingIssueMessage(
			String description, 
			String recommendation, 
			Priority priority) {
		super();
		
		assert description != null;
		setRecommendation(recommendation);
		setDescription(description);
		setType(ObservationType.STYLE_MISSING);
		setKey(generateKey());
	}

	@Override
	public ObservationType getType() {
		return ObservationType.STYLE_MISSING;
	}

}
