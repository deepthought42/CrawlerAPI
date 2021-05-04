package com.looksee.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.PageStateService;
import com.looksee.utils.ContentUtils;

import io.whelk.flesch.kincaid.ReadabilityCalculator;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class ReadabilityAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ReadabilityAudit.class);
	
	@Autowired
	private PageStateService page_state_service;
	
	public ReadabilityAudit() {
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores readability and relevance of content on a page based on the reading level of the content and the keywords used
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state) {
		assert page_state != null;
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		//filter elements that aren't text elements
		int points_earned = 0;
		int max_points = 0;

		//get all element states
		//filter any element state whose text exists within another element
		List<ElementState> og_text_elements = new ArrayList<>();
		
		String ada_compliance = "Even though there are no ADA compliance requirements specifically for" + 
				" this category, reading level needs to be taken into consideration when" + 
				" writing content and paragraphing. ";
			
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		for(ElementState element: elements) {
			if(element.getName().contentEquals("button") || element.getName().contentEquals("a") || element.getOwnedText().isEmpty() || element.getAllText().split(" ").length <= 3) {
				continue;
			}
			boolean is_child_text = false;
			for(ElementState element2: elements) {
				if(element2.getKey().contentEquals(element.getKey())) {
					continue;
				}
				if(!element2.getOwnedText().isEmpty() && element2.getAllText().contains(element.getAllText()) && !element2.getAllText().contentEquals(element.getAllText())) {
					is_child_text = true;
					break;
				}
				else if(element2.getAllText().contentEquals(element.getAllText()) && !element2.getXpath().contains(element.getXpath())) {
					is_child_text = true;
					break;
				}

			}
			
			if(!is_child_text) {
				og_text_elements.add(element);
			}
		}
		
		log.warn("identified   "+og_text_elements.size() + "  og text states");

		
		for(ElementState element : og_text_elements) {
			//List<Sentence> sentences = CloudNLPUtils.extractSentences(all_page_text);
			//Score paragraph_score = calculateParagraphScore(sentences.size());
			double ease_of_reading_score = ReadabilityCalculator.calculateReadingEase(element.getAllText());
			String difficulty_string = ContentUtils.getDifficultyRating(ease_of_reading_score);
			
			if("unknown".contentEquals(difficulty_string) || element.getAllText().split(" ").length <= 10) {
				continue;
			}
			
			String description = "Content is " + difficulty_string + " to read";
			String recommendation = "";
			
			Set<String> labels = new HashSet<>();
			labels.add("written content");
			labels.add("readability");

			int element_points = 0;
			if(ease_of_reading_score >= 80 ) {
				element_points = 3;
			}
			else if(ease_of_reading_score < 80 && ease_of_reading_score >= 70) {
				element_points = 2;
			}
			else if(ease_of_reading_score < 70 && ease_of_reading_score >= 60) {
				element_points = 2;
			}
			else if(ease_of_reading_score < 60 && ease_of_reading_score >= 50) {
				element_points = 1;
			}
			else if(ease_of_reading_score < 50 && ease_of_reading_score >= 30) {
				element_points = 1;
			}
			else if(ease_of_reading_score < 30) {
				element_points = 0;				
			}
			
			points_earned += element_points;
			max_points += 3;
			
			if(element_points < 5) {
				recommendation = "Content is written at a " + ContentUtils.getGradeLevel(ease_of_reading_score) + " reading level, which is considered " + difficulty_string + " to read for most consumers. You can use simpler words and reduce the length of your sentences to make this content more accessible";
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
						Priority.LOW, 
						description,
						recommendation,
						element,
						AuditCategory.CONTENT,
						labels,
						ada_compliance);
				issue_messages.add(issue_message);
			}
		}		

		String why_it_matters = "The way users experience content has changed in the mobile phone era." + 
				" Attention spans are shorter, and users skim through most information." + 
				" Presenting information in small, easy to digest chunks makes their" + 
				" experience easy and convenient. ";
		

		Set<String> labels = new HashSet<>();
		labels.add("content");
		labels.add("readability");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.CONTENT.getShortName());
		

		String description = "";
		return new Audit(AuditCategory.CONTENT,
						 AuditSubcategory.WRITTEN_CONTENT,
						 AuditName.PARAGRAPHING,
						 points_earned,
						 issue_messages,
						 AuditLevel.PAGE,
						 max_points, 
						 page_state.getUrl(),
						 why_it_matters, 
						 description); 
	}


	public static Score calculateSentenceScore(String sentence) {
		//    		for each sentence check that sentence is no longer than 20 words
		String[] words = sentence.split(" ");
		
		if(words.length <= 10) {
			return new Score(2, 2, new HashSet<>());
		}
		else if(words.length <= 20) {
			return new Score(1, 2, new HashSet<>());
		}

		return new Score(0, 2, new HashSet<>());
	}


	public static Score calculateParagraphScore(int sentence_count) {
		if(sentence_count <= 5) {
			return new Score(1, 1, new HashSet<>());
		}

		return new Score(0, 1, new HashSet<>());
		//	  		Verify that there are no more than 5 sentences
		// validate that spacing between paragraphs is at least 2x the font size within the paragraphs
	}
}
