package com.qanairy.models.audit;

import java.util.ArrayList;
import java.util.List;

import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.ColorScheme;

/**
 * Defines the globally required fields for all audits
 */
public class ColorPaletteAudit extends Audit {

	private List<String> colors = new ArrayList<>();
	private List<String> gray_colors = new ArrayList<>();
	private String color_scheme;
	//private ColorPalette color_palette;
	
	public ColorPaletteAudit() {}
	
	/**
	 * 
	 * @param category
	 * @param best_practices
	 * @param ada_compliance_description
	 * @param description
	 * @param color_scheme TODO
	 * @param name
	 */
	public ColorPaletteAudit(AuditCategory category, List<String> best_practices, String ada_compliance_description, String description, AuditSubcategory subcategory, double score, List<String> observations, AuditLevel level, List<String> colors, List<String> gray_colors, ColorScheme color_scheme) {
		setBestPractices(best_practices);
		setAdaCompliance(ada_compliance_description);
		setDescription(description);
		setSubcategory(subcategory);
		setCategory(category);
		setScore(score);
		setObservations(observations);
		setColors(colors);
		setGrayColors(gray_colors);
		setLevel(level);
		setColorScheme(color_scheme);
		setKey(generateKey());
	}

	public ColorPaletteAudit clone() {
		return new ColorPaletteAudit(getCategory(), getBestPractices(), getAdaCompliance(), getDescription(), getSubcategory(), getScore(), getObservations(), getLevel(), getColors(), getGrayColors(), getColorScheme());
	}

	public List<String> getColors() {
		return colors;
	}

	public void setColors(List<String> colors) {
		this.colors = colors;
	}

	public List<String> getGrayColors() {
		return gray_colors;
	}

	public void setGrayColors(List<String> gray_colors) {
		this.gray_colors = gray_colors;
	}

	public ColorScheme getColorScheme() {
		return ColorScheme.create(color_scheme);
	}

	public void setColorScheme(ColorScheme color_scheme) {
		this.color_scheme = color_scheme.toString();
	}

	/*
	public ColorPalette getColorPalette() {
		return color_palette;
	}

	public void setColorPalette(ColorPalette color_palette) {
		this.color_palette = color_palette;
	}
	*/
}
