package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
public class DomainColorPaletteAudit extends DomainAudit{
	private static Logger log = LoggerFactory.getLogger(DomainColorPaletteAudit.class);

	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	public DomainColorPaletteAudit() {
		super(AuditCategory.COLOR_MANAGEMENT, getAuditDescription(), AuditSubcategory.COLOR_PALETTE);
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
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 */
	@Override
	public double execute(List<Audit> audits) {
		assert audits != null;
		
		List<String> observations = new ArrayList<>();
		double overall_score = 0.0;

		Map<String, Boolean> gray_colors = new HashMap<String, Boolean>();
		Map<String, Boolean> colors = new HashMap<>();
		
		for(Audit audit : audits) {
			//retrieve gray colors color of
			if(AuditSubcategory.COLOR_PALETTE.equals(audit.getSubcategory())) {
				ColorPaletteAudit color_palette_audit = ((ColorPaletteAudit)audit);
				for(String color : color_palette_audit.getGrayColors()){
					gray_colors.put(color, Boolean.TRUE);
				}
				
				for(String color : color_palette_audit.getColors()){
					colors.put(color, Boolean.TRUE);
				}
			}
		}
		
		gray_colors.remove(null);
		colors.remove(null);
		System.out.println("colors found :: "+colors);
		log.warn("Total filtered colors found (Includes shades, excludes transparency, and gray) ... "+colors.size());
		log.warn("filtered colors :: "+colors);
		log.warn("Total grayscale colors ... "+gray_colors.size());
		log.warn("gray colors :: "+gray_colors);
		
		//TEMP SOLUTION score by how many primary colors are used. TODO replace with scoring based on color scheme
		
		if(colors.size() < 3) {
			setScore(1.0);
		}
		else if(colors.size() > 3) {
			setScore(2.0);
		}
		else if(colors.size() == 3) {
			setScore(3.0);
		}
		
		
		
		
		//score colors found against scheme
		//setGrayColors(new ArrayList<>(gray_colors.keySet()));
		//setColors(new ArrayList<>(filtered_colors.keySet()));
		setKey(generateKey());
		
		return getScore();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DomainAudit clone() {
		DomainColorPaletteAudit audit = new DomainColorPaletteAudit();
		audit.setScore(getScore());
		audit.setKey(getKey());
		return audit;
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


