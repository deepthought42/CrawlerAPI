package com.qanairy.models.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.enums.ColorScheme;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;


/**
 * A observation of potential error for a given color palette 
 */
public class ColorPaletteObservation extends Observation{
	
	@Relationship(type = "HAS")
	private List<PaletteColor> palette_colors = new ArrayList<>();
	
	private List<String> colors = new ArrayList<>();
	private String color_scheme;
	
	public ColorPaletteObservation() {}
	
	public ColorPaletteObservation(
			List<PaletteColor> palette, 
			ColorScheme scheme, 
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			Priority priority, 
			Set<String> recommendations, 
			Set<String> labels
	) {
		setPaletteColors(palette);
		setColors(palette);
		setColorScheme(scheme);
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setLabels(labels);
		setKey(this.generateKey());
	}

	public List<String> getColors() {
		return colors;
	}

	public void setColors(List<PaletteColor> palette_colors) {
		for(PaletteColor color : palette_colors) {
			this.colors.add(color.getPrimaryColor());
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

	@Override
	public ObservationType getType() {
		return ObservationType.COLOR_PALETTE;
	}
}
