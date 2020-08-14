package com.qanairy.models.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.ogm.annotation.Properties;

import com.qanairy.models.Element;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.ColorScheme;
import com.qanairy.models.enums.ObservationType;


/**
 * A observation of potential error for a given {@link Element element} 
 */
public class ColorPaletteObservation extends LookseeObject implements Observation{
	private String description;
	
	@Properties
	private Map<String, Set<String>> palette = new HashMap<>();
	private List<String> colors = new ArrayList<>();
	private List<String> gray_colors = new ArrayList<>();
	private String color_scheme;
	
	public ColorPaletteObservation() {}
	
	public ColorPaletteObservation(Map<String, Set<String>> palette, List<String> colors, List<String> gray_colors, ColorScheme scheme, String description) {
		setPalette(palette);
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
		return "colorPaletteObservation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(  colors.toString() + gray_colors.toString() + this.getDescription() );
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

	public Map<String, Set<String>> getPalette() {
		return palette;
	}

	public void setPalette(Map<String, Set<String>> palette) {
		this.palette = palette;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public ObservationType getType() {
		return ObservationType.COLOR_PALETTE;
	}
}
