package com.looksee.models.designsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.AudienceProficiency;
import com.looksee.models.enums.WCAGComplianceLevel;

/**
 * Defines a design system for use in defining and evaluating standards based on 
 * the settings withing the design system
 */
@NodeEntity
public class DesignSystem extends LookseeObject{

	private String wcag_compliance_level;
	private String audience_proficiency;
	
	private List<String> allowed_image_characteristics;
	
	@Relationship(type="USES")
	private ColorPalette palette;

	public DesignSystem() {
		wcag_compliance_level = WCAGComplianceLevel.AA.toString();
		audience_proficiency = AudienceProficiency.GENERAL.toString();
		allowed_image_characteristics = new ArrayList<String>();
		palette = new ColorPalette();
	}
	
	public ColorPalette getPalette() {
		return palette;
	}

	public void setPalette(ColorPalette palette) {
		this.palette = palette;
	}
	
	public WCAGComplianceLevel getWcagComplianceLevel() {
		return WCAGComplianceLevel.create(wcag_compliance_level);
	}

	public void setWcagComplianceLevel(WCAGComplianceLevel wcag_compliance_level) {
		this.wcag_compliance_level = wcag_compliance_level.toString();
	}

	public AudienceProficiency getAudienceProficiency() {
		return AudienceProficiency.create(audience_proficiency);
	}

	/**
	 * sets the reading and topic proficiency level 
	 * 
	 * @param audience_proficiency {@link AudienceProficiency} string value
	 */
	public void setAudienceProficiency(AudienceProficiency audience_proficiency) {
		this.audience_proficiency = audience_proficiency.toString();
	}
	
	@Override
	public String generateKey() {
		return "designsystem"+UUID.randomUUID();
	}

	public List<String> getAllowedImageCharacteristics() {
		return allowed_image_characteristics;
	}

	public void setAllowedImageCharacteristics(List<String> allowed_image_characteristics) {
		this.allowed_image_characteristics = allowed_image_characteristics;
	}
}
