package com.qanairy.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ContentUtils {
	private static Logger log = LoggerFactory.getLogger(ContentUtils.class);

	public static String getGradeLevel(double ease_of_reading_score) {
		if(ease_of_reading_score >= 90) {
			return "5 grade";
		}
		else if(ease_of_reading_score < 90 && ease_of_reading_score >= 80) {
			return "6 grade";
		}
		else if(ease_of_reading_score < 80 && ease_of_reading_score >= 70) {
			return "7 grade";
		}
		else if(ease_of_reading_score < 70 && ease_of_reading_score >= 60) {
			return "8th and 9th grades";
		}
		else if(ease_of_reading_score < 60 && ease_of_reading_score >= 50) {
			return "10th to 12th grades";
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			return "college";
		}
		else if(ease_of_reading_score < 30 && ease_of_reading_score >= 10) {
			return "college graduate";
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			return "professional";
		}
		return "unknown";
	}
	
	public static String getDifficultyRating(double ease_of_reading_score) {
		if(ease_of_reading_score >= 90) {
			return "easy";
		}
		else if(ease_of_reading_score < 90 && ease_of_reading_score >= 80) {
			return "easy";
		}
		else if(ease_of_reading_score < 80 && ease_of_reading_score >= 70) {
			return "fairly easy";
		}
		else if(ease_of_reading_score < 70 && ease_of_reading_score >= 60) {
			return "mildly difficulty";
		}
		else if(ease_of_reading_score < 60 && ease_of_reading_score >= 50) {
			return "fairly difficult";
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			return "difficult";
		}
		else if(ease_of_reading_score < 30 && ease_of_reading_score >= 10) {
			return "very difficult";
		}
		else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
			return "extremely difficult";
		}
		return "unknown";
	}
	
	


}
