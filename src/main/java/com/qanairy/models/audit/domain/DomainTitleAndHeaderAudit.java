package com.qanairy.models.audit.domain;

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
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.audit.PageObservation;
import com.qanairy.models.audit.Score;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;
import com.qanairy.services.DomainService;
import com.qanairy.utils.ElementStateUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class DomainTitleAndHeaderAudit implements IExecutableDomainAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainTitleAndHeaderAudit.class);

	@Relationship(type="FLAGGED")
	private List<Element> flagged_elements = new ArrayList<>();
	
	
	@Autowired
	private DomainService domain_service;

	
	public DomainTitleAndHeaderAudit() {}

	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(Domain domain) {
		assert domain != null;
		
		List<Observation> observations = new ArrayList<>();
		List<Page> pages = domain_service.getPages(domain.getHost());

		Score title_score = scorePageTitles(pages);
		Score favicon_score = scoreFavicon(pages);
		Score heading_score = scoreHeadings(pages);
		
		observations.addAll(title_score.getObservations());
		observations.addAll(favicon_score.getObservations());
		observations.addAll(heading_score.getObservations());
		
		int points = title_score.getPointsAchieved() + favicon_score.getPointsAchieved() + heading_score.getPointsAchieved();
		int max_points = title_score.getMaxPossiblePoints() + favicon_score.getMaxPossiblePoints() + heading_score.getMaxPossiblePoints();
		
		log.warn("TITLE FONT AUDIT SCORE   ::   "+points +" / " +max_points);
		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, AuditSubcategory.TITLES, points, observations, AuditLevel.DOMAIN, max_points);
	}

	
	private Score scoreHeadings(List<Page> pages) {
		assert pages != null;

		int points_achieved = 0;
		int max_points = 0;
		Set<Observation> observations = new HashSet<>();
		for(Page page : pages) {
			//generate score for ordered and unordered lists and their headers
			Score list_score = scoreOrderedListHeaders(page);
			points_achieved += list_score.getPointsAchieved();
			max_points += list_score.getMaxPossiblePoints();
			observations.addAll(list_score.getObservations());
			
			//score text elements and their headers
			Score text_block_header_score = scoreTextElementHeaders(page);
			points_achieved += text_block_header_score.getPointsAchieved();
			max_points += text_block_header_score.getMaxPossiblePoints();
			observations.addAll(text_block_header_score.getObservations());
		}
		
		
		return new Score(points_achieved, max_points, observations);
	}

	private Score scoreOrderedListHeaders(Page page) {
		assert page != null;
		int score = 0;
		int max_points = 0;
		
		Document html_doc = Jsoup.parse(page.getSrc());
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

	
	private Score scoreTextElementHeaders(Page page) {
		assert page != null;
		
		int score = 0;
		int max_points = 0;
		
		Document html_doc = Jsoup.parse(page.getSrc());
		
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
			else {
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
						break;
					}
				}
			}
		}
		
		log.warn("Headings score ::    "+score);
		log.warn("Headings max score :::  "+max_points);
		return new Score(score, max_points, new HashSet<>());
	}

	private Score scoreFavicon(List<Page> pages) {
		assert pages != null;
		
		int points = 0;
		Set<Observation> observations = new HashSet<>();

		//find all pages for domain
		for(Page page : pages) {
			//find most recent page state
			//score title of page state
			if(hasFavicon(page)) {
				points += 1;
			}
			else {
				observations.add(new PageObservation(page, "pages without titles"));
				points += 0;				
			}
		}
		
		return new Score(points, pages.size(), observations);
	}

	/**
	 * Checks if a {@link PageState} has a favicon defined
	 * @param page_state
	 * @return
	 */
	private boolean hasFavicon(Page page_state) {
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
	private Score scorePageTitles(List<Page> pages) {
		assert pages != null;
		
		Set<Observation> observations = new HashSet<>();
		int points = 0;
		
		//find all pages for domain
		for(Page page : pages) {
			//score title of page state
			if(page.getTitle() != null && !page.getTitle().isEmpty()) {
				points += 1;
			}
			else {
				observations.add(new PageObservation(page, "pages without titles"));
				points += 0;				
			}
		}
		return new Score(points, pages.size(), observations);
	}
}