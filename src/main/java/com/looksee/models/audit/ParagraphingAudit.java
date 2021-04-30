package com.looksee.models.audit;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.cloud.language.v1.Sentence;
import com.looksee.gcp.CloudNLPUtils;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class ParagraphingAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ParagraphingAudit.class);
	
	@Autowired
	private	PageStateService page_state_service;
	
	public ParagraphingAudit() {
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Scores links on a page based on if the link has an href value present, the url format is valid and the 
	 *   url goes to a location that doesn't produce a 4xx error 
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state) {
		assert page_state != null;

		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		//get all elements that are text containers
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		//filter elements that aren't text elements
		List<ElementState> element_list = BrowserUtils.getTextElements(elements);
		
		int points_earned = 0;
		int max_points = 0;
		
		for(ElementState element : element_list) {
			String text_block = element.getOwnedText();
			
			//    parse text block into paragraph chunks(multiple paragraphs can exist in a text block)
			String[] paragraphs = text_block.split("\n");
			for(String paragraph : paragraphs) {
				
				try {
					List<Sentence> sentences = CloudNLPUtils.extractSentences(paragraph);
					Score score = calculateSentenceScore(sentences, element);
					points_earned += score.getPointsAchieved();
					max_points += score.getMaxPossiblePoints();
					issue_messages.addAll(score.getIssueMessages());
					/*
					
					for(Sentence sentence : sentences) {
						System.err.println("sentence :: " + sentence.getText().getContent());
						
						Score sentence_score = calculateSentenceScore(sentence.getText().getContent());
						points_earned += sentence_score.getPointsAchieved();
						max_points += sentence_score.getMaxPossiblePoints();
						if(sentence_score.getPointsAchieved() == 0) {
							String recommendation = "Try reducing the size of the sentence or breaking it up into multiple sentences";
							String description = "Sentence is too long";
							ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																			Priority.MEDIUM, 
																			description, 
																			recommendation, 
																			element,
																			AuditCategory.CONTENT,
																			labels,
																			ada_compliance);
							issue_messages.add(issue_message);
						}						
					}
					*/
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			// validate that spacing between paragraphs is at least 2x the font size within the paragraphs
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
						 
		//the contstant 6 in this equation is the exact number of boolean checks for this audit
	}


	public static Score calculateSentenceScore(List<Sentence> sentences, ElementState element) {
		//    		for each sentence check that sentence is no longer than 20 words
		int points_earned = 0;
		int max_points = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Set<String> labels = new HashSet<>();
		labels.add("written content");
		String ada_compliance = "Even though there are no ADA compliance requirements specifically for" + 
				" this category, reading level needs to be taken into consideration when" + 
				" writing content and paragraphing. ";
		
		for(Sentence sentence : sentences) {
			System.err.println("sentence :: " + sentence.getText().getContent());
			
			String[] words = sentence.getText().getContent().split(" ");
			
			if(words.length > 25) {

				//return new Score(1, 1, new HashSet<>());
				String recommendation = "Try reducing the size of the sentence or breaking it up into multiple sentences";
				String description = "Sentence is too long";
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.MEDIUM, 
																description, 
																recommendation, 
																element,
																AuditCategory.CONTENT,
																labels,
																ada_compliance);
				
				issue_messages.add(issue_message);
				
				points_earned += 0;
				max_points += 1;
			}
			else {
				points_earned += 1;
				max_points += 1;
			}

		}
		return new Score(points_earned, max_points, issue_messages);					
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
