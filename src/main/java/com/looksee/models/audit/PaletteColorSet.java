package com.looksee.models.audit;

import java.util.List;

import com.looksee.models.LookseeObject;

/**
 * Represents a primary color and it's secondary colors for use in a palette
 */
public class PaletteColorSet extends LookseeObject {

	private ColorData primary;
	private List<ColorData> secondary;
	
	public PaletteColorSet(ColorData primary, List<ColorData> secondary) {
		setPrimary(primary);
		setSecondary(secondary);
	}
	
	@Override
	public String generateKey() {
		// TODO Auto-generated method stub
		return primary.rgb()+secondary;
	}
	
	@Override
	public String toString() {
		return primary.rgb()+secondary;
	}

	public ColorData getPrimary() {
		return primary;
	}

	public void setPrimary(ColorData primary) {
		this.primary = primary;
	}

	public List<ColorData> getSecondary() {
		return secondary;
	}

	public void setSecondary(List<ColorData> secondary) {
		this.secondary = secondary;
	}
}
