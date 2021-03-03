package com.qanairy.models.audit;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.cloud.language.v1.Sentence;
import com.looksee.gcp.CloudNLPUtils;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class ParagraphingAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ParagraphingAudit.class);
	
	@Autowired
	private	PageStateService page_state_service;

	List<Observation> sentence_observations =  new ArrayList<>();
	List<Observation> paragraph_observations =  new ArrayList<>();	
	
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

		List<ElementState> good_sentence_observations = new ArrayList<>();
		List<ElementState> meh_sentence_observations = new ArrayList<>();
		List<ElementState> poor_sentence_observations = new ArrayList<>();

		
		List<ElementState> good_paragraph_observations = new ArrayList<>();
		List<ElementState> poor_paragraph_observations = new ArrayList<>();
		
		//get all elements that are text containers
		List<ElementState> elements = page_state_service.getElementStates(page_state.getKey());
		//filter elements that aren't text elements
		List<ElementState> element_list = BrowserUtils.getTextElements(elements);
		int points_earned = 0;
		int max_points = 0;
		
		for(ElementState element : element_list) {
			String text_block = element.getText();
			
			//    parse text block into paragraph chunks(multiple paragraphs can exist in a text block)
			String[] paragraphs = text_block.split("\n");
			for(String paragraph : paragraphs) {
				
				try {
					List<Sentence> sentences = CloudNLPUtils.extractSentences(paragraph);
					Score paragraph_score = calculateParagraphScore(sentences.size());

					if(paragraph_score.getPointsAchieved() == 1) {
						good_paragraph_observations.add(element);
					}
					else if(paragraph_score.getPointsAchieved() == 0) {
						poor_paragraph_observations.add(element);
					}
					
					points_earned += paragraph_score.getPointsAchieved();
					max_points += paragraph_score.getMaxPossiblePoints();
					for(Sentence sentence : sentences) {
						System.err.println("sentence :: " + sentence.getText().getContent());
						
						Score sentence_score = calculateSentenceScore(sentence.getText().getContent());
						if(sentence_score.getPointsAchieved() == 2) {
							good_sentence_observations.add(element);
						}
						else if(sentence_score.getPointsAchieved() == 1) {
							meh_sentence_observations.add(element);
						}
						else if(sentence_score.getPointsAchieved() == 0) {
							poor_sentence_observations.add(element);
						}
						points_earned += sentence_score.getPointsAchieved();
						max_points += sentence_score.getMaxPossiblePoints();						
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			// validate that spacing between paragraphs is at least 2x the font size within the paragraphs
		}
		
		List<Observation> observations = new ArrayList<>();
		/*
		if(!good_paragraph_observations.isEmpty()) {
			observations.add(new ElementStateObservation(good_paragraph_observations, "Great job keeping these text blocks to under 5 sentences"));
		}
		*/

		if(!poor_paragraph_observations.isEmpty()) {
			observations.add(new ElementStateObservation(poor_paragraph_observations, "Paragraphs with more than 5 sentences"));
		}
		
		//Sentence observations
		/*
		if(!good_sentence_observations.isEmpty()) {
			observations.add(new ElementStateObservation(good_sentence_observations, "Great job keeping sentences to under 10 words!!!"));
		}
		*/
		
		if(!meh_sentence_observations.isEmpty()) {
			observations.add(new ElementStateObservation(meh_sentence_observations, "Sentences between 10 and 20 words long"));
		}
		
		if(!poor_sentence_observations.isEmpty()) {
			observations.add(new ElementStateObservation(poor_sentence_observations, "Sentences with over 20 words"));
		}
		
		
		String why_it_matters = "The way users experience content has changed in the mobile phone era." + 
				" Attention spans are shorter, and users skim through most information." + 
				" Presenting information in small, easy to digest chunks makes their" + 
				" experience easy and convenient. ";
		
		String ada_compliance = "Even though there are no ADA compliance requirements specifically for" + 
				" this category, reading level needs to be taken into consideration when" + 
				" writing content and paragraphing. ";

		return new Audit(AuditCategory.CONTENT,
						 AuditSubcategory.WRITTEN_CONTENT, 
						 AuditName.PARAGRAPHING, 
						 points_earned, 
						 observations, 
						 AuditLevel.PAGE, 
						 max_points, 
						 page_state.getUrl(),
						 why_it_matters,
						 ada_compliance); 
						 
		//the contstant 6 in this equation is the exact number of boolean checks for this audit
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
