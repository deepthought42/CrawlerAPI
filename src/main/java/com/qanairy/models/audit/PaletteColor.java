package com.qanairy.models.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.ogm.annotation.Properties;

import com.qanairy.models.LookseeObject;

/**
 * Contains data for individual palette primary colors and the shades, tints, and tones associated with them
 *
 */
public class PaletteColor extends LookseeObject {

	private String primary_color;
	private float primary_color_percent;
	
	@Properties
	private Map<String, String> tints_shades_tones = new HashMap<>();
	
	public PaletteColor() {}
	
	public PaletteColor(String primary_color, float primary_color_percent, Map<String, String> tints_shades_tones) {
		setPrimaryColor(primary_color.trim());
		setPrimaryColorPercent(primary_color_percent);
		addTintsShadesTones(tints_shades_tones);
		setKey(generateKey());
	}

	public String getPrimaryColor() {
		return primary_color;
	}

	private void setPrimaryColor(String primary_color) {
		this.primary_color = primary_color;
	}

	public float getPrimaryColorPercent() {
		return primary_color_percent;
	}

	private void setPrimaryColorPercent(float primary_color_percent) {
		this.primary_color_percent = primary_color_percent;
	}

	public Map<String, String> getTintsShadesTones() {
		return tints_shades_tones;
	}

	public void addTintsShadesTones(Map<String, String> tints_shades_tones) {
		this.tints_shades_tones.putAll(tints_shades_tones);
	}

	@Override
	public String generateKey() {
		List<String> sorted_keys = new ArrayList<>(tints_shades_tones.keySet());
		Collections.sort(sorted_keys);
		return "palettecolor"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( primary_color + sorted_keys );
	}
}
