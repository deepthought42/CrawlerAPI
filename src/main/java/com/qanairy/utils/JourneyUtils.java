package com.qanairy.utils;

import com.qanairy.models.ElementState;
import com.qanairy.models.journeys.ElementInteractionStep;
import com.qanairy.models.journeys.Journey;
import com.qanairy.models.journeys.Step;

public class JourneyUtils {

	public static ElementState extractLastElement(Journey journey) {
		
		for(int idx = journey.getOrderedKeys().size()-1; idx >= 0; idx--) {
			
			String step_key = journey.getOrderedKeys().get(idx);
			
			//get step from step set
			for(Step step : journey.getSteps()) {
				if(step.getKey().equals(step_key) && step instanceof ElementInteractionStep ) {
						//get last element interation journey step
						return ((ElementInteractionStep)step).getElement();
				}
			}
		}
		
		return null;
	}

}
