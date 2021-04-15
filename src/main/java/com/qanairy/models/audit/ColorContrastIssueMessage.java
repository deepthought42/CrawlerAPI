package com.qanairy.models.audit;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.ElementState;
import com.qanairy.models.enums.Priority;


/**
 * A observation of potential error for a given color palette 
 */
public class ColorContrastIssueMessage extends UXIssueMessage{
	@Relationship(type = "FOR")
	private ElementState element;
	
	private double contrast;
	private String foreground_color;
	private String background_color;
	
	public ColorContrastIssueMessage() {}
	
	/**
	 * Constructs new instance
	 * 
	 * @param priority
	 * @param recommendation
	 * @param contrast
	 * @param foreground_color
	 * @param background_color
	 * @param element
	 * 
	 * 
	 * @pre priority != null
	 * @pre recommendation != null
	 * @pre !recommendation.isEmpty()
	 * @pre element != null
	 * @pre foreground_color != null
	 * @pre !foreground_color.isEmpty()
	 * @pre assert background_color != null
	 * @pre !background_color.isEmpty()
	 * 
	 */
	public ColorContrastIssueMessage(
			Priority priority, 
			String recommendation,
			double contrast,
			String foreground_color,
			String background_color,
			ElementState element
	) {
		assert priority != null;
		assert recommendation != null;
		assert !recommendation.isEmpty();
		assert element != null;
		assert foreground_color != null;
		assert !foreground_color.isEmpty();
		assert background_color != null;
		assert !background_color.isEmpty();

		setPriority(priority);
		setContrast(contrast);
		setForegroundColor(foreground_color);
		setBackgroundColor(background_color);
		setElement(element);
		setKey(this.generateKey());
	}

	public double getContrast() {
		return contrast;
	}

	public void setContrast(double contrast) {
		this.contrast = contrast;
	}

	public String getForegroundColor() {
		return foreground_color;
	}

	public void setForegroundColor(String foreground_color) {
		this.foreground_color = foreground_color;
	}

	public String getBackgroundColor() {
		return background_color;
	}

	public void setBackgroundColor(String background_color) {
		this.background_color = background_color;
	}

	public ElementState getElement() {
		return element;
	}

	public void setElement(ElementState element) {
		this.element = element;
	}
}
