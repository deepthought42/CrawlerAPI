package com.looksee.utils;

import com.looksee.models.ElementState;
import com.looksee.models.journeys.ElementInteractionStep;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.Step;

public class JourneyUtils {

	public static ElementState extractLastElement(Journey journey) {
		
		for(int idx = journey.getOrderedIds().size()-1; idx >= 0; idx--) {
			
			long step_id = journey.getOrderedIds().get(idx);
			
			//get step from step set
			for(Step step : journey.getSteps()) {
				if(step.getId() == step_id && step instanceof ElementInteractionStep ) {
						//get last element interation journey step
						return ((ElementInteractionStep)step).getElement();
				}
			}
		}
		
		return null;
	}

}
