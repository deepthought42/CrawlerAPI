package com.qanairy.utils;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import com.qanairy.models.ElementState;

public class ElementStateUtils {
	/*
	public static List<WebElement> fitlerNonDisplayedElements(List<ElementState> elements) {
		List<ElementState> filtered_elems = new ArrayList<>();
		for(ElementState elem : elements){
			if(elem.isDisplayed()){
				filtered_elems.add(elem);
			}
		}
		return filtered_elems;
	}
*/
	
	public static List<ElementState> filterElementsWithNegativePositions(List<ElementState> elements) {
		List<ElementState> filtered_elements = new ArrayList<>();

		for(ElementState element : elements){
			if(element.getXLocation() >= 0 && element.getYLocation() >= 0){
				filtered_elements.add(element);
			}
		}

		return filtered_elements;
	}

	public static List<ElementState> filterNotVisibleInViewport(int x_offset, int y_offset, List<ElementState> elements, Dimension viewport_size) {
		List<ElementState> filtered_elements = new ArrayList<>();

		for(ElementState element : elements){
			if(isElementVisibleInPane( x_offset, y_offset, element, viewport_size)){
				filtered_elements.add(element);
			}
		}

		return filtered_elements;
	}

	/**
	 * Filters out html, body, script and link tags
	 *
	 * @param web_elements
	 * @return
	 */
	public static List<WebElement> filterStructureTags(List<WebElement> web_elements) {
		List<WebElement> elements = new ArrayList<>();
		for(WebElement element : web_elements){
			if(element.getTagName().equals("html") || element.getTagName().equals("body")
					|| element.getTagName().equals("link") || element.getTagName().equals("script")
					|| element.getTagName().equals("title") || element.getTagName().equals("meta")
					|| element.getTagName().equals("head")){
				continue;
			}
			elements.add(element);
		}
		return elements;
	}
	
	
	public static boolean isElementVisibleInPane(int x_offset, int y_offset, ElementState elem, Dimension viewport_size){
		int x = elem.getXLocation();
		int y = elem.getYLocation();

		int height = elem.getHeight();
		int width = elem.getWidth();

		return x >= x_offset && y >= y_offset && (x+width) <= (viewport_size.getWidth()+x_offset)
				&& (y+height) <= (viewport_size.getHeight()+y_offset);
	}
}
