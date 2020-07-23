package com.qanairy.models.audit.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.ColorData;
import com.qanairy.models.audit.ColorPaletteObservation;
import com.qanairy.models.audit.ColorPaletteUtils;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.ColorScheme;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainColorPaletteAudit implements IExecutableDomainAudit{
	private static Logger log = LoggerFactory.getLogger(DomainColorPaletteAudit.class);

	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	public DomainColorPaletteAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}
	
	private static String getAuditDescription() {
		return "The colors that make up the style guide of your website.";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("A brand guide typically should have 3 colours. You may use different shades of your brand colours to diversify the visual aesthetics of the website but be sure to remain consistent.");
		best_practices.add("This can be further divided into primary, secondary, tertiary colors.");
		
		return best_practices;
	}
	
	private static String getAdaDescription() {
		return "";
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 */
	@Override
	public Audit execute(List<Audit> audits) {
		assert audits != null;
		
		List<Observation> observations = new ArrayList<>();

		Map<String, Boolean> gray_colors = new HashMap<String, Boolean>();
		Map<String, Boolean> colors = new HashMap<>();
		
		for(Audit audit : audits) {
			//retrieve gray colors color of
			if(audit.getSubcategory().equals(AuditSubcategory.COLOR_PALETTE)) {
				for(Observation observation : audit.getObservations() ) {
					if(observation instanceof ColorPaletteObservation) {
						ColorPaletteObservation palette_observation = (ColorPaletteObservation)observation;
						for(String color : palette_observation.getGrayColors()){
							gray_colors.put(color, Boolean.TRUE);
						}
						
						for(String color : palette_observation.getColors()){
							colors.put(color, Boolean.TRUE);
						}
					}
				}
			}
		}
		
		gray_colors.remove(null);
		colors.remove(null);
		log.warn("colors found :: "+colors);
		log.warn("gray colors :: "+gray_colors);
		
		//generate palette, identify color scheme and score how well palette conforms to color scheme
		Map<ColorData, Set<ColorData>> palette = ColorPaletteUtils.extractPalette(colors.keySet());
		for(ColorData primary_color : palette.keySet()) {
			log.warn("Primary color :: "+primary_color.rgb() + "   ;   " + primary_color.getLuminosity());
		}
		ColorScheme scheme = ColorPaletteUtils.getColorScheme(palette);
		int score = ColorPaletteUtils.getPaletteScore(palette, scheme);
		
		//score colors found against scheme
		setGrayColors(new ArrayList<>(gray_colors.keySet()));
		setColors(new ArrayList<>(colors.keySet()));
		 
		
		return new Audit(AuditCategory.COLOR_MANAGEMENT, AuditSubcategory.COLOR_PALETTE, score, observations, AuditLevel.DOMAIN, 3);
	}

	

	public List<String> getGrayColors() {
		return gray_colors;
	}

	public void setGrayColors(List<String> gray_colors) {
		this.gray_colors = gray_colors;
	}

	public List<String> getColors() {
		return colors;
	}

	public void setColors(List<String> colors) {
		this.colors = colors;
	}
}


