package com.looksee.models.audit;

import java.util.List;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.ColorScheme;

/**
 * Contains the set of primary colors and their associated secondary colors used in the color palette
 */
public class ColorPalette extends LookseeObject {
	private String color_scheme;
	
	private List<PaletteColorSet> palette_colors;
	
	public ColorPalette(ColorScheme scheme, List<PaletteColorSet> palette_colors) {
		setColorScheme(scheme);
		setPaletteColors(palette_colors);
	}
	
	@Override
	public String generateKey() {
		// TODO Auto-generated method stub
		return color_scheme + ":" + palette_colors;
	}
	
	public ColorScheme getColorScheme() {
		return ColorScheme.create(color_scheme);
	}
	
	public void setColorScheme(ColorScheme color_scheme) {
		this.color_scheme = color_scheme.toString();
	}

	public List<PaletteColorSet> getPaletteColors() {
		return palette_colors;
	}

	public void setPaletteColors(List<PaletteColorSet> palette_colors) {
		this.palette_colors = palette_colors;
	}
}
