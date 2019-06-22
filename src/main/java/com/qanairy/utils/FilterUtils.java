package com.qanairy.utils;

import java.util.ArrayList;
import java.util.List;

import com.qanairy.models.ElementState;

public class FilterUtils {
	public static List<ElementState> filterElementsWithNegativePositions(List<ElementState> element_states) {
		List<ElementState> elements = new ArrayList<>();

		for(ElementState element : element_states){
			if(element.getXLocation() >= 0 && element.getYLocation() >= 0){
				elements.add(element);
			}
		}

		return elements;
	}
}
