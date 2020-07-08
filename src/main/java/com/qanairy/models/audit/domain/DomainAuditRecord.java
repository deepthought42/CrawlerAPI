package com.qanairy.models.audit.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.LookseeObject;
import com.qanairy.models.audit.Audit;

import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Defines a set of audits that were ran together
 */
public class DomainAuditRecord extends LookseeObject{
	@Properties
	private Map<String, Double> category_scores = new HashMap<String, Double>();
	
	@Relationship(type = "HAS")
	private Set<Audit> audits;
	
	
	public DomainAuditRecord(){}
	
	/**
	 * @param audits {@linkplain Set} of {@link Audit audits}
	 */
	public DomainAuditRecord(Set<Audit> audits) {
		setAudits(audits);
		calculateCategoryScores(audits);
		setKey(generateKey());
	}

	private void calculateCategoryScores(Set<Audit> audits) {
		//extract color palette
		Set<Audit> palette_audits = new HashSet<>();
		for(Audit audit : audits) {
			if(AuditSubcategory.COLOR_PALETTE.equals(audit.getSubcategory())){
				palette_audits.add(audit);
			}
		}
		
		//extract text contrast
		Set<Audit> text_contrast_audit = new HashSet<>();
		for(Audit audit : audits) {
			if(AuditSubcategory.TEXT_BACKGROUND_CONTRAST.equals(audit.getSubcategory())){
				text_contrast_audit.add(audit);
			}
		}
		
		//calculate color palette overall score
		double overall_palette_score = 0;
		for(Audit audit : palette_audits) {
			overall_palette_score += audit.getScore();
		}
		overall_palette_score = overall_palette_score / palette_audits.size();
		
		//calculate color palette overall score
		double overall_text_contrast_score = 0;
		for(Audit audit : text_contrast_audit) {
			overall_text_contrast_score += audit.getScore();
		}
		overall_text_contrast_score = overall_text_contrast_score / text_contrast_audit.size();
		
		//calculate color management
		category_scores.put(AuditCategory.COLOR_MANAGEMENT.toString(), ((overall_palette_score+overall_text_contrast_score)/2.0) );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		String audit_key = "";
		for(Audit audit : audits) {
			audit_key += audit.getKey();
		}
		return "domain_audit_record:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(audit_key);
	}

	public Set<Audit> getAudits() {
		return audits;
	}

	public void setAudits(Set<Audit> audits) {
		this.audits = audits;
	}
}
