package com.qanairy.models.audit.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.ogm.annotation.Properties;

import com.qanairy.models.Element;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.ObservationType;


/**
 * A observation of potential error for a given {@link Element element} 
 */
public class DomainColorPaletteObservation extends Observation{
	
	@Properties
	private Map<String, Set<String>> palette = new HashMap<>();
	private List<String> colors = new ArrayList<>();
	private List<String> gray_colors = new ArrayList<>();
	private Set<String> color_schemes = new HashSet<>();
	
	public DomainColorPaletteObservation() {}
	
	public DomainColorPaletteObservation(
			Map<String, Set<String>> palette, 
			List<String> colors, 
			List<String> gray_colors, 
			Set<String> schemes, 
			String description
	) {
		setPalette(palette);
		setColors(colors);
		setGrayColors(gray_colors);
		setColorScheme(schemes);
		setDescription(description);
		setType(ObservationType.COLOR_PALETTE);
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

	public Set<String> getColorSchemes() {
		return this.color_schemes;
	}

	public void setColorScheme(Set<String> color_scheme) {
		this.color_schemes.addAll(color_scheme);
	}

	public Map<String, Set<String>> getPalette() {
		return palette;
	}

	public void setPalette(Map<String, Set<String>> palette) {
		this.palette = palette;
	}

	@Override
	public ObservationType getType() {
		return ObservationType.COLOR_PALETTE;
	}
}
