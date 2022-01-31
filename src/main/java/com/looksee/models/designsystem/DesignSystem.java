package com.looksee.models.designsystem;

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

	private WCAGComplianceLevel wcag_compliance_level;
	private AudienceProficiency audience_proficiency;
	
	@Relationship(type="USES")
	private ColorPalette palette;

	public DesignSystem() {
		wcag_compliance_level = WCAGComplianceLevel.AA;
		audience_proficiency = AudienceProficiency.GENERAL;
		palette = new ColorPalette();
	}
	
	public ColorPalette getPalette() {
		return palette;
	}

	public void setPalette(ColorPalette palette) {
		this.palette = palette;
	}
	
	public String getWcagComplianceLevel() {
		return wcag_compliance_level.toString();
	}

	public void setWcagComplianceLevel(String wcag_compliance_level) {
		this.wcag_compliance_level = WCAGComplianceLevel.create(wcag_compliance_level);
	}

	public String getAudienceProficiency() {
		return audience_proficiency.toString();
	}

	/**
	 * sets the reading and topic proficiency level 
	 * 
	 * @param audience_proficiency {@link AudienceProficiency} string value
	 */
	public void setAudienceProficiency(String audience_proficiency) {
		this.audience_proficiency = AudienceProficiency.create(audience_proficiency);
	}
	
	@Override
	public String generateKey() {
		return "designsystem"+UUID.randomUUID();
	}
}
