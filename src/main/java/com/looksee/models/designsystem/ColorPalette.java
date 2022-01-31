package com.looksee.models.designsystem;

import java.util.HashSet;
import java.util.Set;

import com.looksee.models.LookseeObject;
import com.looksee.models.audit.PaletteColorSet;
import com.looksee.models.enums.ColorScheme;

/**
 * Contains the set of primary colors and their associated secondary colors used in the color palette
 */
public class ColorPalette extends LookseeObject {
	private ColorScheme color_scheme;
	
	private Set<PaletteColorSet> palette_colors;
	
	public ColorPalette() {
		setColorScheme(ColorScheme.UNKNOWN.getShortName());
		setPaletteColors(new HashSet<>());
	}
	
	public ColorPalette(ColorScheme scheme, Set<PaletteColorSet> palette_colors) {
		setColorScheme(scheme.getShortName());
		setPaletteColors(palette_colors);
	}
	
	@Override
	public String generateKey() {
		return color_scheme + ":" + palette_colors;
	}
	
	public String getColorScheme() {
		return color_scheme.getShortName();
	}
	
	public void setColorScheme(String color_scheme) {
		this.color_scheme = ColorScheme.create(color_scheme);
	}

	public Set<PaletteColorSet> getPaletteColors() {
		return palette_colors;
	}

	public void setPaletteColors(Set<PaletteColorSet> palette_colors) {
		this.palette_colors = palette_colors;
	}
}
