package com.qanairy.models.audit;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;



import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.utils.BrowserUtils;

/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
public class LinksAudit extends InformationArchitectureAudit {
	
	//@Autowired
	//private PageStateService page_state_service;
	
	private List<ElementState> links_without_href =  new ArrayList<>();
	private List<ElementState> invalid_links = new ArrayList<>();
	private List<ElementState> dead_links = new ArrayList<>();
	
	public LinksAudit() {
		super(buildBestPractices(), getAdaDescription(), getAuditDescription(), "links");
	}
	
	private static String getAuditDescription() {
		return "A hyperlink that takes you to a new location should be reactive and result in the user navigating to an existing webpage";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("Label should describe what the link is. \"Click here\" should not be used.");
		
		return best_practices;
	}
	
	private static String getAdaDescription() {
		return "\r\n" + 
				"2.4.4 - Descriptive Links\r\n" + 
				"The purpose of each link can be determined from the link text alone or from the link text together with its programmatically determined link context.\r\n" + 
				"\r\n" + 
				"2.4.7 - Visible Focus\r\n" + 
				"When an interactive element (link, button, form field, selectable element, etc.) receives focus, a visual indicator shows so a user can see what element they are currently on.";
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
	public double execute(PageState page_state, String user_id) throws MalformedURLException, URISyntaxException {
		assert page_state != null;
		assert user_id != null;
		
		//List<ElementState> link_elements = page_state_service.getLinkElementStates(user_id, page_state.getKey());
		List<ElementState> link_elements = new ArrayList<>();
		for(ElementState element : page_state.getElements()) {
			if(element.getName().equalsIgnoreCase("a")) {
				link_elements.add(element);
			}
		}
		
		List<String> observations = new ArrayList<>();
		double overall_score = 0.0;
		//score each link element
		for(ElementState link : link_elements) {
			int score = 0;
			
			//does element have an href value?
			String link_href = null;
			if(link.getAttribute("href") != null) {
				link_href = link.getAttribute("href").getVals().get(0);
				URI uri = new URI(link_href);
				
				if(!uri.isAbsolute()) {
					URL url = new URL(page_state.getUrl());
					link_href = url.getProtocol()+"://"+url.getHost() + link_href;
				}
				score++;
			}
			else {
				links_without_href.add(link);
			}
			
			//is element link a valid url?
			URL url = null;
			try {
				url = new URL(link_href);
				score++;
			} catch (MalformedURLException e) {
				invalid_links.add(link);
				e.printStackTrace();
			}
			
			
			//Does link have a valid URL? yes(1) / No(0)
			try {
				if(BrowserUtils.doesUrlExist(url)) {
					score++;
				}
				else {
					dead_links.add(link);
				}
			} catch (IOException e) {
				dead_links.add(link);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//TODO : Does link have a hover styling? yes(1) / No(0)
			
			//TODO : Is link label relevant to destination url or content? yes(1) / No(0)
				//TODO :does link text exist in url? 
				//TODO :does target content relate to link?
			overall_score += score/3.0;
		}
		
		if(!links_without_href.isEmpty()) {
			observations.add("We found " + links_without_href.size() + " links without an 'href' value");
		}
		
		if(!invalid_links.isEmpty()) {
			observations.add("We found " + invalid_links.size() + " invalid links");
		}
		
		if(!dead_links.isEmpty()) {
			observations.add("We found " + dead_links.size() + " dead links");
		}
		
		observations.add("");
		setObservations(observations);
		setScore( overall_score/link_elements.size() );
		setKey(generateKey());
		
		return getScore();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Audit clone() {
		LinksAudit audit = new LinksAudit();
		audit.setScore(getScore());
		audit.setKey(getKey());
		return audit;
	}
}
