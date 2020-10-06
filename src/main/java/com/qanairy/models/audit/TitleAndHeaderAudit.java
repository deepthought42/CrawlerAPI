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
import com.qanairy.models.enums.AuditSubcategory;
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
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, AuditSubcategory.TITLES, points, observations, AuditLevel.PAGE, max_points);
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
		if(hasFavicon(page_state)) {
			points += 1;
		}
		else {
			observations.add(new PageStateObservation(page_state, "Favicon is missing."));
			points += 0;			
		}
		
		return new Score(points, max_points, observations);
	}

	/**
	 * Checks if a {@link PageState} has a favicon defined
	 * @param page
	 * @return
	 */
	public boolean hasFavicon(PageState page_state) {
		assert page_state != null;
		
		Document doc = Jsoup.parse(page_state.getSrc());
		Elements link_elements = doc.getElementsByTag("link");
		for(Element element: link_elements) {
			if("icon".contentEquals(element.attr("rel")) && !element.attr("href").isEmpty()) {
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
			observations.add(new PageStateObservation(page_state, "pages without titles"));
			points += 0;				
		}
		
		return new Score(points, max_points, observations);
	}
}