package com.qanairy.models.audit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Defines a set of audits that were ran together
 */
public class DomainAuditRecord extends LookseeObject{
	@Properties
	private Map<String, Double> category_scores = new HashMap<String, Double>();
	
	@Relationship(type = "HAS")
	private Set<AuditRecord> audit_records;
	
	
	public DomainAuditRecord(){}
	
	/**
	 * @param audits {@linkplain Set} of {@link Audit audits}
	 */
	public DomainAuditRecord(Set<AuditRecord> audit_records) {
		setAudits(audit_records);
		calculateCategoryScores(audit_records);
		setKey(generateKey());
	}

	private void calculateCategoryScores(Set<AuditRecord> audits) {
		//extract color palette
		Set<ColorPaletteAudit> palette_audits = new HashSet<>();
		for(AuditRecord record: audits) {
			for(Audit audit : record.getAudits()) {
				if(AuditSubcategory.COLOR_PALETTE.equals(audit.getSubcategory())){
					palette_audits.add((ColorPaletteAudit)audit);
				}
			}
		}
		
		//extract text contrast
		Set<TextColorContrastAudit> text_contrast_audit = new HashSet<>();
		for(AuditRecord record: audits) {
			for(Audit audit : record.getAudits()) {
				if(AuditSubcategory.TEXT_BACKGROUND_CONTRAST.equals(audit.getSubcategory())){
					text_contrast_audit.add((TextColorContrastAudit)audit);
				}
			}
		}
		
		//calculate color palette overall score
		double overall_palette_score = 0;
		for(ColorPaletteAudit audit : palette_audits) {
			overall_palette_score += audit.getScore();
		}
		overall_palette_score = overall_palette_score / palette_audits.size();
		
		//calculate color palette overall score
		double overall_text_contrast_score = 0;
		for(TextColorContrastAudit audit : text_contrast_audit) {
			overall_text_contrast_score += audit.getScore();
		}
		overall_text_contrast_score = overall_text_contrast_score / text_contrast_audit.size();
		
		//calculate color management
		category_scores.put(AuditCategory.COLOR_MANAGEMENT.toString(), ((overall_palette_score+overall_text_contrast_score)/2.0) );
	}

	private Double calculateColorPalette(String url, List<ColorPaletteAudit> audits) {
		//find all color palette management

		double overall_score = 0;
		//collect all colors from audit records with color_palette subtype
		for(ColorPaletteAudit record: audits) {
			overall_score += record.getScore();
		}
		
		//group colors based on which colors are closes to each other in hue? or maybe saturation? to identify primary colors
		//Identify color scheme type by how many primary colors are used
				
		return overall_score/audits.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		String audit_key = "";
		for(AuditRecord audit : audit_records) {
			audit_key += audit.getKey();
		}
		return "domain_audit_record:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(audit_key);
	}

	public Set<AuditRecord> getAudits() {
		return audit_records;
	}

	public void setAudits(Set<AuditRecord> audit_records) {
		this.audit_records = audit_records;
	}
}
