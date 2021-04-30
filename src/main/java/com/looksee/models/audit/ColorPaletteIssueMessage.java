package com.looksee.models.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ColorScheme;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;


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
	 * @param description TODO
	 * @param recommendation
	 * @param colors
	 * @param palette_colors
	 * @param color_scheme
	 * @param category TODO
	 * @param labels TODO
	 * @param wcag_compliance TODO
	 * @pre priority != null;
	 * @pre recommendation != null;
	 * @pre !recommendation.isEmpty();
	 * @pre colors != null;
	 * @pre palette_colors != null;
	 * @pre color_scheme != null;
	 */
	public ColorPaletteIssueMessage(
			Priority priority, 
			String description, 
			String recommendation, 
			List<String> colors, 
			List<PaletteColor> palette_colors, 
			ColorScheme color_scheme, 
			AuditCategory category, 
			Set<String> labels, 
			String wcag_compliance
	) {
		assert priority != null;
		assert recommendation != null;
		assert !recommendation.isEmpty();
		assert colors != null;
		assert palette_colors != null;
		assert color_scheme != null;
		assert category != null;
		assert labels != null;
		
		setPriority(priority);
		setDescription(description);
		setRecommendation(recommendation);
		setColors(colors);
		setPaletteColors(palette_colors);
		setColorScheme(color_scheme);
		setCategory(category);
		setLabels(labels);
		setType(ObservationType.COLOR_PALETTE);
		setWcagCompliance(wcag_compliance);
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
