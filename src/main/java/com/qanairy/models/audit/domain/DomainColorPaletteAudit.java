package com.qanairy.models.audit.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.ColorData;
import com.qanairy.models.audit.ColorPaletteObservation;
import com.qanairy.models.audit.ColorPaletteUtils;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.audit.PaletteColor;
import com.qanairy.models.audit.Score;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.ColorScheme;
import com.qanairy.models.enums.Priority;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;
import com.qanairy.services.ObservationService;
import com.qanairy.services.PaletteColorService;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainColorPaletteAudit implements IExecutableDomainAudit{
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainColorPaletteAudit.class);

	private List<String> gray_colors = new ArrayList<>();
	private List<String> colors = new ArrayList<>();
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private ObservationService observation_service;
	
	@Autowired
	private PaletteColorService palette_color_service;
	
	public DomainColorPaletteAudit() {}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 */
	@Override
	public Audit execute(Domain domain) {
		assert domain != null;
		
		List<Observation> observations = new ArrayList<>();
		
		//get all color palette audits associated with most recent audit record for domain host
		Set<Audit> color_palette_audits = domain_service.getMostRecentAuditRecordColorPaletteAudits(domain.getHost());
		int points = 0;
		int max_points = 0;
		//List<PaletteColor> palette_colors = new ArrayList<>();
		Map<String, Boolean> schemes_recognized = new HashMap<>();
		List<String> color_strings = new ArrayList<>();
		List<String> gray_color_strings = new ArrayList<>();

		//iterate over color palette audits
		for(Audit audit : color_palette_audits) {
			List<Observation> page_audit_observations = audit_service.getObservations(audit.getKey());
			//extract colors into global color map
			//calculate global color usage percentages using page state audits
			for(Observation observation : page_audit_observations) {
				if(observation instanceof ColorPaletteObservation) {
					ColorPaletteObservation palette_observation = (ColorPaletteObservation)observation;
					schemes_recognized.put(palette_observation.getColorScheme().getShortName(), Boolean.TRUE);
					
					color_strings.addAll(palette_observation.getColors());
					//palette_colors.addAll(palette_color_service.saveAll(palette_observation.getPaletteColors()));
				}
			}
			
			points += audit.getPoints();
			max_points += audit.getTotalPossiblePoints();
		}
		
		if(schemes_recognized.size() == 1) {
			points += 3;
		}
		max_points += 3;
		
		Map<String, Boolean> color_map = new HashMap<>();
		for(String color : color_strings) {
			color_map.put(color, Boolean.TRUE);
		}
		
		List<ColorData> color_data_list = new ArrayList<>();
		for(String color : color_map.keySet()) {
			color_data_list.add(new ColorData(color));
		}
		
		/*
		Map<String, PaletteColor> palette_map = new HashMap<>();
		for(PaletteColor color : palette_colors) {
			palette_map.put(color.getPrimaryColor(), color);
		}
		palette_colors = new ArrayList<>(palette_map.values());
		 */

		List<PaletteColor> palette = ColorPaletteUtils.extractPalette(color_data_list);
		ColorScheme scheme = ColorPaletteUtils.getColorScheme(palette);
		
		Score score = ColorPaletteUtils.getPaletteScore(palette, scheme);
		points += score.getPointsAchieved();
		max_points += score.getMaxPossiblePoints();
		
		//score colors found against scheme
		setGrayColors(new ArrayList<>(gray_color_strings));
		setColors(new ArrayList<>(color_map.keySet()));

		String why_it_matters = "Studies have found that it takes 90 seconds for a customer to form an" + 
				"opinion on a product. 62–90% of that interaction is determined by the" + 
				"color of the product alone." + 
				"Color impacts how a user feels when they interact with your website; it is" + 
				"key to their experience. The right usage of colors can brighten a website" + 
				"and communicates the tone of your brand. Furthermore, using your brand" + 
				"colors consistently makes the website appear cohesive and collected," + 
				"while creating a sense of familiarity for the user.";
		
		String ada_compliance = "There are no ADA compliance guidelines regarding the website color" + 
				"palette. However, keeping a cohesive color palette allows you to create" + 
				"a webpage easy for everyone to read. ";
		
		Set<String> labels = new HashSet<>();
		labels.add("accessibility");
		labels.add("color");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		
		ColorPaletteObservation palette_observation = new ColorPaletteObservation(
																palette, 
																scheme, 
																"This is a color scheme description",
																why_it_matters,
																ada_compliance, 
																Priority.HIGH,
																new HashSet<>(), 
																labels,
																categories);
		
		
		observations.add(observation_service.save(palette_observation));

		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.COLOR_MANAGEMENT,
						 AuditName.COLOR_PALETTE, 
						 points, 
						 observations, 
						 AuditLevel.DOMAIN, 
						 max_points,
						 domain.getHost());
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


