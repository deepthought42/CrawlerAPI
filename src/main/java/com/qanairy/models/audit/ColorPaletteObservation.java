package com.qanairy.models.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.enums.ColorScheme;
import com.qanairy.models.enums.ObservationType;


/**
 * A observation of potential error for a given color palette 
 */
public class ColorPaletteObservation extends Observation{
	
	@Relationship(type = "HAS")
	private List<PaletteColor> palette_colors = new ArrayList<>();
	
	private List<String> colors = new ArrayList<>();
	private List<String> gray_colors = new ArrayList<>();
	private String color_scheme;
	
	public ColorPaletteObservation() {}
	
	public ColorPaletteObservation(
			List<PaletteColor> palette, 
			List<String> colors, 
			List<String> gray_colors, 
			ColorScheme scheme, 
			String description
	) {
		setPaletteColors(palette);
		setColors(colors);
		setGrayColors(gray_colors);
		setColorScheme(scheme);
		setDescription(description);
		setKey(this.generateKey());
	}
	
	@Override
	public String generateKey() {
		Collections.sort(colors);
		Collections.sort(gray_colors);
		return "colorPaletteObservation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( colors.toString() + gray_colors.toString() + this.getDescription() );
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
		this.color_scheme = color_scheme.getShortName();
	}

	public List<PaletteColor> getPaletteColors() {
		return palette_colors;
	}

	public void setPaletteColors(List<PaletteColor> palette) {
		this.palette_colors.addAll(palette);
	}

	@Override
	public ObservationType getType() {
		return ObservationType.COLOR_PALETTE;
	}
}
