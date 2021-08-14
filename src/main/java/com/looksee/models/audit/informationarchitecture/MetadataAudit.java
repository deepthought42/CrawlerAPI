package com.looksee.models.audit.informationarchitecture;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.looksee.api.MessageBroadcaster;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.IExecutablePageStateAudit;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.utils.ContentUtils;

import io.whelk.flesch.kincaid.ReadabilityCalculator;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class MetadataAudit implements IExecutablePageStateAudit {
	private static Logger log = LoggerFactory.getLogger(MetadataAudit.class);
	
	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record) {
		assert page_state != null;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		//List<PageVersion> pages = domain_service.getPages(domain.getHost());

		Score title_score = scoreTitle(page_state);
		Score description_score = scoreDescription(page_state);
		Score refresh_score = scoreRefreshes(page_state);
		// Score keyword_score = scoreKeywords(page_state);   TODO: uncomment once keywords are part of dataset
		
		issue_messages.addAll(title_score.getIssueMessages());
		issue_messages.addAll(description_score.getIssueMessages());
		issue_messages.addAll(refresh_score.getIssueMessages());
		//issue_messages.addAll(keyword_score.getIssueMessages()); TODO: uncomment once keywords are part of dataset
		
		for(UXIssueMessage issue_msg : issue_messages) {
			MessageBroadcaster.sendIssueMessage(page_state.getId(), issue_msg);
		}
		
		int normalized_title_score = (title_score.getPointsAchieved()/title_score.getMaxPossiblePoints())*100;
		int normalized_description_score = (description_score.getPointsAchieved()/description_score.getMaxPossiblePoints())*100;
		int normalized_refresh_score = (refresh_score.getPointsAchieved()/refresh_score.getMaxPossiblePoints())*100;

		int points = normalized_title_score + normalized_description_score + normalized_refresh_score;
		int max_points = 300;
		
		log.warn("METADATA AUDIT SCORE   ::   "+points +" / " +max_points);
		String why_it_matters = "Metadata tells search engines what your web page has to offer. By using metadata correctly, you can boost your relevancy in search results. Metadata provides search engines with the most important information about your web pages, including titles and descriptions.";
		String description = "";
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
						 AuditSubcategory.SEO,
						 AuditName.METADATA,
						 points,
						 issue_messages,
						 AuditLevel.PAGE,
						 max_points,
						 page_state.getUrl(), 
						 why_it_matters, 
						 description, 
						 page_state,
						 false);
	}

	/**
	 * Checks if title matches SEO requirements
	 * 
	 * 		1. Between 50-60 characters long
	 * 		2. contains Brand name
	 * 		3. no duplicates across pages (full-site)
	 * 		4. accurately describes content of page
	 * 		5. doesn't keyword stuff
	 * 		6. H1 headline doesn't match title tag content
	 * 	
	 * Resources: 
	 * 
	 * 		1)  https://www.w3.org/TR/UNDERSTANDING-WCAG20/navigation-mechanisms-title.html
	 * 		2)  
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreTitle(PageState page_state) {
		assert page_state != null;

		int points_achieved = 0;
		int max_points = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		//extract title from page_state
		String page_title = page_state.getTitle();
		
		// check 1. Between 50-60 characters long
		if(page_title.length() > 50 || page_title.length() < 60) {
			points_achieved++;
		}
		else {
			String recommendation = "";
			String title = "";
			
			if(page_title.length() < 50) {
				recommendation = "Make your title longer so that it has between 50 and 60 characters";
				title = "Title is too short";
			}
			else if(page_title.length() > 60) {
				recommendation = "Make your title shorter so that it has between 50 and 60 characters";
				title = "Title is too long";
			}
			
			Priority priority = Priority.MEDIUM;
			String description = "For best results you should aim to keep your description between 50 and 60 characters long. Google typically displays the first 50–60 characters of a title tag, however there isn't an exact character limit, because characters can vary in width and Google's display titles max out (currently) at 600 pixels.";
			
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			
			String wcag_compliance = "There is no WCAG requirement for SEO techniques";
			
			Set<String> labels = new HashSet<>();
			
			String why_it_matters = "When your title is too short or too long then users aren't able to easily identify what to expect from a page.";
			
			
			issue_messages.add(new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title));
		}
		max_points++;
		
		// check 2. contains Brand name - These details aren't available yet. uncomment and finish once they are.
		/*
		if(!page_title.containsBrandName()) {
			points_achieved++;
		}
		else {
			new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
		}
		max_points++;
		 */
		
		// 4. accurately describes content of page
		// TODO: FINISH CODE IMPLEMENTATION BELOW ONCE NECESSARY MACHINE LEARNING ALGORITHM IS DETERMINED
		/*
		String page_content = PageUtils.extractContent();
		if(describesContentOfPage( page_title, page_content )) {
			points_achieved++;
		}
		else {
			String recommendation = "Change title to more accurately reflect the content of the page";
			String title = "Description doesn't reflect content of page";
			
			Priority priority = Priority.MEDIUM;
			
			String description = "It's important that your title accurately describe the content on the page.";
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			
			String wcag_compliance = "There are no specific WCAG guidelines. However, titles that are readable and relate to the content within the page helps users that rely on assistive technology to quickly identify pages while having multiple tabs open";
			Set<String> labels = new HashSet<>();
			
			String why_it_matters = "When your title doesn't align with the content, search engines may penalize your page ranking";
			
			new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
		}
		max_points++;
		*/

		// 5. doesn't use keyword stuffing
		// TODO: FINISH CODE IMPLEMENTATION BELOW ONCE NECESSARY MACHINE LEARNING ALGORITHM IS DETERMINED
		/*
		if( usesKeywordStuffing(page_title) ) {
			points_achieved++;
		}
		else {
			new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
		}
		max_points++;

		// 6. H1 headline doesn't match title tag content
		if(doesHeadlineMatchTitle(page_state)) {
			points_achieved++;
		}
		else {
			new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
		}
		max_points++;
		*/
		return new Score(points_achieved, max_points, issue_messages);
	}

	/**
	 * 
	 * @param page_title
	 * @param page_content
	 * 
	 * @pre page_title != null
	 * @pre page title is not empty
	 * @pre page_content != null
	 * @pre page content is not empty
	 * 
	 * 
	 * @return true if title desribes content of page
	 */
	private boolean describesContentOfPage(String page_title, String page_content) {
		assert page_title != null;
		assert !page_title.isEmpty();
		assert page_content != null;
		assert !page_content.isEmpty();
		
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Generates a score for headers found on the page
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreKeywords(PageState page_state) {
		assert page_state != null;

		int points_achieved = 0;
		int max_points = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		//extract keywords from metadata tags for keywords
		//add keywords to page_state
		//if meta tag with name="keywords" exists then add 1 to score
		//if keywords is not empty then add 1 to score
		//if keywords does NOT contains duplicates then add 1 to score
		
		return new Score(points_achieved, max_points, issue_messages);
	}

	/**
	 * INCOMPLETE: PLEASE FINISH ME
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreDescription(PageState page_state) {
		assert page_state != null;
		int score = 0;
		int max_points = 0;
		
		int description_count = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();

		Document html_doc = Jsoup.parse(page_state.getSrc());
		//review element tree top down to identify elements that own text.
		Elements meta_elements = html_doc.getElementsByTag("meta");
		//List<Element> jsoup_elements = body_elem.get(0).children();
		for(Element element : meta_elements) {
			//if element has attribute name="description" then add 1 to score
			if(element.attr("name").contentEquals("description")) {
				score++;
				description_count++;
				//if element with type description contains text then add 1 to score
				if(!element.text().isEmpty()) {
					score++;

					//if element with type description contains text with length < 160 add 1 to score ( Google recommended description length - https://moz.com/learn/seo/meta-description)
					if(element.text().length() < 160) {
						score++;
					}
					else {
						String recommendation = "Try to be more concise with your meta description and make sure the description is no longer than 160 characters";
						Priority priority = Priority.MEDIUM;
						String description = "The meta description \"" + element.text() + "\" is too short";
						ObservationType type = ObservationType.SEO;
						AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
						String wcag_compliance = "There are no WCAG requirements for this";
						Set<String> labels = new HashSet<>();
						String why_it_matters = "Search engines show the meta description to users when your page shows up in search results. Meta descriptions that are longer than 160 characters get cut off by Search engines and won't be shown to the user";
						String title= "Meta description is too long";
						
						UXIssueMessage issue_msg = new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
						issue_messages.add(issue_msg);
					}
					
					//if element with type description contains text with length > 50 then add 1 to score
					if(element.text().length() > 50) {
						score++;
					}
					else {
						String recommendation = "Add some more context to your description, so that it's easy for a user to understand if they will find what they are looking for on the page";
						Priority priority = Priority.MEDIUM;
						String description = "The meta description \"" + element.text() + "\" is too short";
						ObservationType type = ObservationType.SEO;
						AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
						String wcag_compliance = "There are no WCAG requirements for this";
						Set<String> labels = new HashSet<>();
						String why_it_matters = "Search engines show the meta description to users when your page shows up in search results. Meta descriptions that are too short often lack enough information to know if the content within a search result is going to be helpful.";
						String title= "Meta description is too short";
						
						UXIssueMessage issue_msg = new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
						issue_messages.add(issue_msg);
					}
				
					//if element with type description contains text that is between 5-8th grade reading level then add 1 to score
					double ease_of_reading_score = ReadabilityCalculator.calculateReadingEase(element.text());
					if(ease_of_reading_score >= 70.0) {
						score++;
					}
					else {
						String recommendation = "Simplify the language in your meta description so that it is within the 5-7 grade reading level";
						Priority priority = Priority.MEDIUM;
						String description = "The text \"" + element.text() + 
											"\" is written at the "+ ContentUtils.getReadingGradeLevel(ease_of_reading_score) + 
											 ", which is considered " + ContentUtils.getReadingDifficultyRating(ease_of_reading_score) + " to read";
						ObservationType type = ObservationType.SEO;
						AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
						String wcag_compliance = "There are no WCAG requirements for this";
						Set<String> labels = new HashSet<>();
						String why_it_matters = "When users are reviewing search results they don't read everything. Instead they scan the results."
												+ "When your meta description is too difficult to read, it makes it difficult to understand if your page can help them at a quick glance."
												+ "This often results on the user choosing another option that with a better meta description";
						String title= "Meta description is " + ContentUtils.getReadingDifficultyRating(ease_of_reading_score) + " to read";
						
						UXIssueMessage issue_msg = new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
						issue_messages.add(issue_msg);
					}
				}
				else {
					String recommendation = "Add a description to the meta html element. For best results your description should be between 50 and 160 characters in length";
					Priority priority = Priority.MEDIUM;
					String description = "The meta description tag on your page is empty";
					ObservationType type = ObservationType.SEO;
					AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
					String wcag_compliance = "There are no WCAG requirements for this";
					Set<String> labels = new HashSet<>();
					String why_it_matters = "Search engines show the meta description to users when your page shows up in search results. Having a description helps users understand if they can find what they are looking for on any given webpage.";
					String title= "Meta description is empty";
					
					UXIssueMessage issue_msg = new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
					issue_messages.add(issue_msg);
				}
			}
			max_points += 5;
		}

		if(description_count == 0) {
			max_points = 1;
			String recommendation = "Add a meta html element with a description of the purpose of your page. Example : <meta name='description'>your description here</meta>";
			Priority priority = Priority.MEDIUM;
			String description = "Meta html tags allow you to provide a description that can be used by search engines to help users easily understand what they can accomplish with each page";
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			String wcag_compliance = "There are no WCAG requirements for this";
			Set<String> labels = new HashSet<>();
			String why_it_matters = "Search engines show the meta description to users when your page shows up in search results. Having a description helps users understand if they can find what they are looking for on any given webpage.";
			String title= "Meta description not found";
			
			UXIssueMessage issue_msg = new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
			issue_messages.add(issue_msg);
			
		}
		if(description_count > 1) {
			score = score / description_count;
			
			String recommendation = "Remove extraneous meta description tags";
			Priority priority = Priority.LOW;
			String description = description_count + " meta description tags were found.";
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			String wcag_compliance = "There are no WCAG requirements for this";
			Set<String> labels = new HashSet<>();
			String why_it_matters = "Search engines will only show one meta description to users. Having more than 1 meta description doesn't help, and may actually hurt your search ranking";
			String title= "Too many meta descriptions found";
			
			UXIssueMessage issue_msg = new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title);
			issue_messages.add(issue_msg);
		}
		
		return new Score(score, max_points, issue_messages);
	}
	
	private Score scoreRefreshes(PageState page_state) {
		assert page_state != null;
		
		int score = 0;
		int max_points = 1;
		Set<UXIssueMessage> issue_messages = new HashSet<>();

		Document html_doc = Jsoup.parse(page_state.getSrc());
		//review element tree top down to identify elements that own text.
		Elements meta_elements = html_doc.getElementsByTag("meta");
		//List<Element> jsoup_elements = body_elem.get(0).children();
		int refresh_element_count = 0;
		for(Element element : meta_elements) {
			//if element has attribute name="refresh" then add 1 refresh_element_count
			if(element.hasAttr("name") && element.attr("name").contentEquals("refresh")) {
				meta_elements.add(element);
			}
		}
		
		if(refresh_element_count == 0) {
			score += 1;
		}
		else {
			String recommendation = "Remove the meta tag with the attribute name='refresh'";
			Priority priority = Priority.HIGH;
			String description = "Meta tag with name=\"refresh\" was found";
			ObservationType type = ObservationType.SEO;
			AuditCategory category = AuditCategory.INFORMATION_ARCHITECTURE;
			String wcag_compliance = "There are no WCAG requirements for this";
			Set<String> labels = new HashSet<>();
			String why_it_matters = "Meta html tags with name=\"refresh\" are discouraged because consistent page refreshes can be disruptive to the experience as well as making a page highly difficult to interact with for people that rely on assistive technologies";
			String title= "Meta refresh tag found";
			
			issue_messages.add(new UXIssueMessage(recommendation, priority, description, type, category, wcag_compliance, labels, why_it_matters, title));
		}
		max_points++;
		
		return new Score(score, max_points, issue_messages);
	}
}