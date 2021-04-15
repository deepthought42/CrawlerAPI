package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditName;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.ElementStateUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TitleAndHeaderAudit implements IExecutablePageStateAudit {
	private static Logger log = LoggerFactory.getLogger(TitleAndHeaderAudit.class);
	
	public TitleAndHeaderAudit() {}

	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state) {
		assert page_state != null;
		List<Observation> observations = new ArrayList<>();
		//List<PageVersion> pages = domain_service.getPages(domain.getHost());

		Score title_score = scorePageTitles(page_state);
		Score favicon_score = scoreFavicon(page_state);
		Score heading_score = scoreHeadings(page_state);
		
		observations.addAll(title_score.getObservations());
		observations.addAll(favicon_score.getObservations());
		observations.addAll(heading_score.getObservations());
		
		int points = title_score.getPointsAchieved() + favicon_score.getPointsAchieved() + heading_score.getPointsAchieved();
		int max_points = title_score.getMaxPossiblePoints() + favicon_score.getMaxPossiblePoints() + heading_score.getMaxPossiblePoints();
		
		log.warn("TITLE FONT AUDIT SCORE   ::   "+points +" / " +max_points);
		
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
						 AuditSubcategory.SEO,
						 AuditName.TITLES,
						 points,
						 observations,
						 AuditLevel.PAGE,
						 max_points,
						 page_state.getUrl());
	}

	/**
	 * Generates a score for headers found on the page
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreHeadings(PageState page_state) {
		assert page_state != null;

		int points_achieved = 0;
		int max_points = 0;
		Set<Observation> observations = new HashSet<>();
		
		//generate score for ordered and unordered lists and their headers
		Score list_score = scoreOrderedListHeaders(page_state);
		points_achieved += list_score.getPointsAchieved();
		max_points += list_score.getMaxPossiblePoints();
		observations.addAll(list_score.getObservations());
		
		//score text elements and their headers
		Score text_block_header_score = scoreTextElementHeaders(page_state);
		points_achieved += text_block_header_score.getPointsAchieved();
		max_points += text_block_header_score.getMaxPossiblePoints();
		observations.addAll(text_block_header_score.getObservations());	
		
		return new Score(points_achieved, max_points, observations);
	}

	/**
	 * INCOMPLETE: PLEASE FINISH ME
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreOrderedListHeaders(PageState page_state) {
		assert page_state != null;
		int score = 0;
		int max_points = 0;
		
		Document html_doc = Jsoup.parse(page_state.getSrc());
		//review element tree top down to identify elements that own text.
		Elements body_elem = html_doc.getElementsByTag("body");
		List<Element> jsoup_elements = body_elem.get(0).children();
		for(Element element : jsoup_elements) {
			//ignore header tags (h1,h2,h3,h4,h5,h6)
			if(ElementStateUtils.isHeader(element.tagName()) || !element.ownText().isEmpty()) {
				continue;
			}
			
			//extract ordered lists
			//does element own text?
			if(ElementStateUtils.isList(element.tagName())) {
				
				//check if element has header element sibling preceding it
			}			
		}
		
		return new Score(score, max_points, new HashSet<>());
	}

	/**
	 * Generates score based on if text elements have an associated header
	 * 
	 * @param page_state
	 * @return
	 */
	private Score scoreTextElementHeaders(PageState page_state) {
		assert page_state != null;
		
		int score = 0;
		int max_points = 0;
		
		Document html_doc = Jsoup.parse(page_state.getSrc());
		
		//review element tree top down to identify elements that own text.
		Elements body_elem = html_doc.getElementsByTag("body");
		List<Element> jsoup_elements = body_elem.get(0).children();
		while(!jsoup_elements.isEmpty()) {
			Element element = jsoup_elements.remove(0);
			//ignore header tags (h1,h2,h3,h4,h5,h6)
			if(ElementStateUtils.isHeader(element.tagName()) || ElementStateUtils.isList(element.tagName())) {
				continue;
			}
			
			//does element own text?
			if(!element.ownText().isEmpty()) {
				jsoup_elements.addAll(element.children());
			}
			else if(!element.text().isEmpty() ){
				//check if element has header element sibling preceding it
				int element_idx = element.elementSiblingIndex();
				Elements sibling_elements = element.siblingElements();
				for(Element sibling : sibling_elements) {
					if(ElementStateUtils.isHeader(sibling.tagName())) {
						//check if sibling has a lower index
						int sibling_idx = sibling.siblingIndex();
						if(sibling_idx < element_idx) {
							score += 3;
						}
						else {
							score += 1;
						}
						max_points += 3;
						log.warn("header found for text as previous sibling :: " + score + " / " + max_points);
						break;
					}
				}
			}
		}
		
		
		log.warn("Headings score ::    "+score);
		log.warn("Headings max score :::  "+max_points);
		return new Score(score, max_points, new HashSet<>());
	}

	/**
	 * Generates score based on if favicon is present
	 * 
	 * @param page_state
	 * @return
	 * 
	 * @pre page_state != null
	 */
	private Score scoreFavicon(PageState page_state) {
		assert page_state != null;
		
		int points = 0;
		int max_points = 1;
		Set<Observation> observations = new HashSet<>();

		//score title of page state
		if(hasFavicon(page_state.getSrc())) {
			points += 1;
			
			//check if favicon is valid by having a valid href value defined
			 //&& !element.attr("href").isEmpty()
			
			//check if resource can actually be reached
		}
		else {
			String description = "favicon is missing";
			String why_it_matters = "The favicon is a small detail with a big impact on engagement. When users leave your site to look at another tab that they have open, the favicon allos them to easily identify the tab that belongs to your service.";
			String ada_compliance = "Nunc nulla odio, accumsan ac mauris quis, efficitur mattis sem. Maecenas mattis non urna nec malesuada. Nullam felis risus, interdum vel turpis non, elementum lobortis nulla. Sed laoreet sagittis maximus. Vestibulum ac sollicitudin lectus, vitae viverra arcu. Donec imperdiet sit amet lorem non tempor. Phasellus velit leo, vestibulum at justo ac, viverra scelerisque massa. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Morbi rutrum nunc et turpis facilisis gravida. Vivamus nec ipsum sed nunc efficitur mattis sed pulvinar metus. Morbi vitae nisi sit amet purus efficitur mattis. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Pellentesque accumsan, nisi eu dignissim convallis, elit libero dictum dui, eu euismod mauris dui nec odio.";
			String recommendation = "";
			
			Set<String> labels = new HashSet<>();
			labels.add("accessibility");
			labels.add("color");
			
			Set<String> categories = new HashSet<>();
			categories.add(AuditCategory.AESTHETICS.toString());
			Set<UXIssueMessage> favicon_issues = new HashSet<>();

			PageStateIssueMessage favicon_issue = new PageStateIssueMessage(
															page_state, 
															recommendation, 
															Priority.HIGH);
			favicon_issues.add(favicon_issue);
			observations.add(new Observation(description, why_it_matters, ada_compliance, ObservationType.PAGE_STATE, labels, categories, favicon_issues));
			points += 0;			
		}
		
		return new Score(points, max_points, observations);
	}

	/**
	 * Checks if a {@link PageState} has a favicon defined
	 * @param page
	 * @return
	 */
	public static boolean hasFavicon(String page_src) {
		assert page_src != null;
		
		Document doc = Jsoup.parse(page_src);
		Elements link_elements = doc.getElementsByTag("link");
		for(Element element: link_elements) {
			if((element.attr("rel").contains("icon"))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Generate a score for page titles across all pages in this domain
	 * @param domain
	 * @return
	 */
	private Score scorePageTitles(PageState page_state) {
		assert page_state != null;
		
		Set<Observation> observations = new HashSet<>();
		int points = 0;
		int max_points = 1;
		String title = BrowserUtils.getTitle(page_state);
		//score title of page state
		if( title != null && !title.isEmpty()) {
			points += 1;
		}
		else {

			String description = "pages without titles";
			String why_it_matters = "Making sure each of your pages has a title is incredibly important for SEO. The title isn't just used to display as the page name in the browser. Search engines also use this information as part of their evaluation.";
			String ada_compliance = "Nunc nulla odio, accumsan ac mauris quis, efficitur mattis sem. Maecenas mattis non urna nec malesuada. Nullam felis risus, interdum vel turpis non, elementum lobortis nulla. Sed laoreet sagittis maximus. Vestibulum ac sollicitudin lectus, vitae viverra arcu. Donec imperdiet sit amet lorem non tempor. Phasellus velit leo, vestibulum at justo ac, viverra scelerisque massa. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Morbi rutrum nunc et turpis facilisis gravida. Vivamus nec ipsum sed nunc efficitur mattis sed pulvinar metus. Morbi vitae nisi sit amet purus efficitur mattis. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Pellentesque accumsan, nisi eu dignissim convallis, elit libero dictum dui, eu euismod mauris dui nec odio.";
			String recommendation = "";
			
			Set<String> labels = new HashSet<>();
			labels.add("accessibility");
			labels.add("color");
			
			Set<String> categories = new HashSet<>();
			categories.add(AuditCategory.AESTHETICS.toString());
			Set<UXIssueMessage> favicon_issues = new HashSet<>();

			PageStateIssueMessage favicon_issue = new PageStateIssueMessage(
															page_state, 
															recommendation, 
															Priority.HIGH);
			favicon_issues.add(favicon_issue);
			observations.add(new Observation(description, why_it_matters, ada_compliance, ObservationType.PAGE_STATE, labels, categories, favicon_issues));

			points += 0;				
		}
		
		return new Score(points, max_points, observations);
	}
}