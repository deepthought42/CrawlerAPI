package com.looksee.utils;

import java.util.Set;

import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.enums.AuditCategory;

public class AuditUtils {

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
		
		double accessibility_score = 0;
		int accessibility_count = 0;
		
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
    		else if(AuditCategory.ACCESSIBILITY.equals(audit.getCategory())) {
    			accessibility_score += (audit.getPoints())/(double)audit.getTotalPossiblePoints();
    			accessibility_count++;
    		}
    	}
    	
    	if(content_count > 0) {
    		content_score = (content_score / (double)content_count) * 100;
    	}
    	if(info_architecture_count > 0) {
    		info_architecture_score = (info_architecture_score / (double)info_architecture_count) * 100;
    	}
    	if(aesthetic_count > 0) {
    		aesthetic_score = (aesthetic_score / (double)aesthetic_count) * 100;
    	}
    	if(accessibility_count > 0) {
        	accessibility_score = (accessibility_score / (double)accessibility_count) * 100;
    	}
    	
    	return new AuditScore(content_score, info_architecture_score, aesthetic_score, interactivity_score, accessibility_score);
    	
	}
	
}
