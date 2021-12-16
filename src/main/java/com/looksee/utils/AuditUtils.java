package com.looksee.utils;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditSubcategory;

public class AuditUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditUtils.class.getName());


	public static double calculateScore(Set<Audit> audits) {
		assert audits != null;
		double score = 0.0;
		int audit_cnt = 0;
				
		for(Audit audit: audits) {
			if(audit.getTotalPossiblePoints() == 0) {
				continue;
			}
			audit_cnt++;
			
			score += ((double)audit.getPoints() / (double)audit.getTotalPossiblePoints());
		}
		
		if(audits.size() == 0) {
			return 0.0;
		}
		return score/(double)audit_cnt;
	}
	
	/**
	 * Reviews set of {@link Audit} and generates audits scores for content,
	 *   information architecture, aesthetics, interactivity and accessibility
	 *   
	 * @param audits
	 * @return
	 */
	public static AuditScore extractAuditScore(Set<Audit> audits) {
		double content_score = 0;
		int content_count = 0;
		
		double info_architecture_score = 0;
		int info_architecture_count = 0;
		
		double aesthetic_score = 0;
		int aesthetic_count = 0;
		
		double interactivity_score = 0;
		int interactivity_count = 0;
		
    	for(Audit audit: audits) {
    		if(audit.getTotalPossiblePoints() == 0) {
    			continue;
    		}
    		
    		if(AuditCategory.CONTENT.equals(audit.getCategory())) {
    			content_score += (audit.getPoints()/(double)audit.getTotalPossiblePoints());
    			content_count++;
    		}
    		else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(audit.getCategory())) {
    			info_architecture_score += (audit.getPoints()/(double)audit.getTotalPossiblePoints());
    			info_architecture_count++;
    		}
    		else if(AuditCategory.AESTHETICS.equals(audit.getCategory())) {
    			aesthetic_score += (audit.getPoints()/(double)audit.getTotalPossiblePoints());
    			aesthetic_count++;
    		}
    	}
    	
    	if(content_count > 0) {
    		content_score = ( content_score / (double)content_count ) * 100;
    	}
    	if(info_architecture_count > 0) {
    		info_architecture_score = ( info_architecture_score / (double)info_architecture_count ) * 100;
    	}
    	if(aesthetic_count > 0) {
    		aesthetic_score = ( aesthetic_score / (double)aesthetic_count ) * 100;
    	}
    	
    	double readability = extractLabelScore(audits, "readability");
    	double spelling_grammar = extractLabelScore(audits, "spelling");
    	double image_quality = extractLabelScore(audits, "images");
    	double alt_text = extractLabelScore(audits, "alt_text");
    	double links = extractLabelScore(audits, "links");
    	double metadata = extractLabelScore(audits, "metadata");
    	double seo = extractLabelScore(audits, "seo");
    	double security = extractLabelScore(audits, "security");
    	double color_contrast = extractLabelScore(audits, "color contrast");
    	double whitespace = extractLabelScore(audits, "whitespace");
    	double accessibility = extractLabelScore(audits, "accessibility");
    	
    	return new AuditScore(content_score,
    							readability,
    							spelling_grammar,
    							image_quality,
    							alt_text,
    							info_architecture_score,
    							links,
    							metadata,
    							seo,
    							security,
    							aesthetic_score,
    							color_contrast, 
    							whitespace, 
    							interactivity_score, 
    							accessibility);
    	
	}

	private static double extractLabelScore(Set<Audit> audits, String label) {
		double score = 0.0;
		int count = 0;
    	for(Audit audit: audits) {
    		for(UXIssueMessage msg: audit.getMessages()) {
    			if(msg.getLabels().contains(label)) {
    				count++;
    				score += (msg.getPoints() / (double)msg.getMaxPoints());
       			}
    		}
    	}
    	
    	if(count <= 0) {
    		return 0.0;
    	}
    	
    	return score / (double)count;
	}

	public static boolean isPageAuditComplete(AuditRecord page_audit_record) {
		return page_audit_record.getAestheticAuditProgress() >= 1 
			&& page_audit_record.getContentAuditProgress() >= 1
			&& page_audit_record.getInfoArchAuditProgress() >= 1
			&& page_audit_record.getDataExtractionProgress() >= 1;
	}

	public static String getExperienceRating(PageAuditRecord audit_record) {
		double score = audit_record.getAestheticAuditProgress();
		score += audit_record.getContentAuditProgress();
		score += audit_record.getInfoArchAuditProgress();
		
		double final_score = score / 3;
		if(final_score >= 80) {
			return "delightful";
		}
		else if(final_score <80.0 && final_score >= 60) {
			return "almost there";
		}
		else {
			return "needs work";
		}
	}
	
	public static boolean isAestheticsAuditComplete(Set<Audit> audits) {
		return audits.size() == 2;
	}

	public static boolean isContentAuditComplete(Set<Audit> audits) {
		return audits.size() == 3;
	}
	
	public static boolean isInformationArchitectureAuditComplete(Set<Audit> audits) {
		return audits.size() == 3;
	}

	public static double calculateSubcategoryScore(Set<Audit> audits, AuditSubcategory subcategory) {
		assert audits != null;
		
		double score = 0.0;
		int audit_cnt = 0;
	
		for(Audit audit: audits) {
			if(audit.getTotalPossiblePoints() == 0 || !subcategory.equals(audit.getSubcategory())) {
				continue;
			}
			audit_cnt++;
			score += ((double)audit.getPoints() / (double)audit.getTotalPossiblePoints());
		}
		
		if(audit_cnt == 0) {
			return -1.0;
		}
		return score / (double)audit_cnt;
	}
}
