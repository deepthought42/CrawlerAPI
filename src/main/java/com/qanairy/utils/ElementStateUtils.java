package com.qanairy.utils;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import com.qanairy.models.ElementState;

public class ElementStateUtils {
	
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

	public static boolean isHeader(String tag_name) {
		return "h1".equalsIgnoreCase(tag_name) 
				|| "h2".equalsIgnoreCase(tag_name)
				|| "h3".equalsIgnoreCase(tag_name)
				|| "h4".equalsIgnoreCase(tag_name)
				|| "h5".equalsIgnoreCase(tag_name)
				|| "h6".equalsIgnoreCase(tag_name);
	}

	public static boolean isTextContainer(ElementState element_state) {
		Document doc = Jsoup.parseBodyFragment(element_state.getOuterHtml());
		Element body = doc.body();
		return !body.ownText().isEmpty();
	}
	
	public static boolean isTextContainer(WebElement element) {
		Document doc = Jsoup.parseBodyFragment(element.getAttribute("outerHTML"));
		Element body = doc.body();
		return !body.ownText().isEmpty();
	}

	public static boolean isList(String tag_name) {
		return "ul".equalsIgnoreCase(tag_name) 
				|| "ol".equalsIgnoreCase(tag_name)
				|| "li".equalsIgnoreCase(tag_name);
	}
}
