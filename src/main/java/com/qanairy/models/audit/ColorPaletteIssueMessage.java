package com.qanairy.models.audit;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.enums.ColorScheme;
import com.qanairy.models.enums.Priority;


/**
 * A observation of potential error for a given color palette 
 */
public class ColorPaletteIssueMessage extends UXIssueMessage{
	
	@Relationship(type = "HAS")
	private List<PaletteColor> palette_colors = new ArrayList<>();
	
	private List<String> colors = new ArrayList<>();
	private String color_scheme;
	
	public ColorPaletteIssueMessage() {}
	
	/**
	 * Constructs new object
	 * 
	 * @param priority
	 * @param recommendation
	 * @param colors
	 * @param palette_colors
	 * @param color_scheme
	 * 
	 * @pre priority != null;
	 * @pre recommendation != null;
	 * @pre !recommendation.isEmpty();
	 * @pre colors != null;
	 * @pre palette_colors != null;
	 * @pre color_scheme != null;
	 */
	public ColorPaletteIssueMessage(
			Priority priority, 
			String recommendation, 
			List<String> colors, 
			List<PaletteColor> palette_colors, 
			ColorScheme color_scheme
	) {
		assert priority != null;
		assert recommendation != null;
		assert !recommendation.isEmpty();
		assert colors != null;
		assert palette_colors != null;
		assert color_scheme != null;
		
		setPriority(priority);
		setRecommendation(recommendation);
		setColors(colors);
		setPaletteColors(palette_colors);
		setColorScheme(color_scheme);
		setKey(this.generateKey());
	}

	public List<String> getColors() {
		return colors;
	}

	public void setColors(List<String> colors) {
		for(String color : colors) {
			this.colors.add(color);
		}
	}

	public ColorScheme getColorScheme() {
		return ColorScheme.create(color_scheme);
	}

	public void setColorScheme(ColorScheme color_scheme) {
		this.color_scheme = color_scheme.getShortName();
	}

	public List<PaletteColor> getPaletteColors() {
		return palette_colors;
	}

	public void setPaletteColors(List<PaletteColor> palette) {
		this.palette_colors.addAll(palette);
	}
}
