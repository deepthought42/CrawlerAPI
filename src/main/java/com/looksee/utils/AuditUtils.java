package com.looksee.utils;

import java.util.Set;

import com.looksee.models.audit.Audit;

public class AuditUtils {

	public static double calculateScore(Set<Audit> audits) {
		assert audits != null;
		double score = 0.0;
		int audit_cnt = 0;
		
		System.err.println("calculating score of audits :: "+audits.size());
		
		for(Audit audit: audits) {
			if(audit.getTotalPossiblePoints() == 0) {
				continue;
			}
			audit_cnt++;
			score += (audit.getPoints() / audit.getTotalPossiblePoints());
		}
		
		return score/audit_cnt;
	}
	
}
