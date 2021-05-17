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
			System.err.println("points earned is " +audit.getPoints());
			System.err.println("total possible points is "+ audit.getTotalPossiblePoints());

			if(audit.getTotalPossiblePoints() == 0) {
				System.err.println("total possible points is 0");
				continue;
			}
			audit_cnt++;
			score += ((double)audit.getPoints() / (double)audit.getTotalPossiblePoints());
			System.err.println("new score calculated :: "+score);
		}
		System.err.println("cumulative score :: "+(score/(double)audit_cnt));	
		return score/(double)audit_cnt;
	}
	
}
