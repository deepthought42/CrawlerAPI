package com.minion.actors;


import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.browsing.Browser;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.RenderedPageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.message.CrawlActionMessage;
import com.qanairy.services.DomainService;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.PageService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * Web Crawler actor that can handle crawling a website in various ways. 
 * 
 *  1. Links only crawling ( captures links and finds pages) - CrawlAction
 *  2. Interaction crawl (crawl a specific page and look for element interactions)
 *  3. Journey crawl (explore a domain in browser as a "user" and record journey paths)
 *  
 */
@Component
@Scope("prototype")
public class WebCrawlerActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(WebCrawlerActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private ActorSystem actor_system;

	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageService page_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
	}

	//re-subscribe when restart
	@Override
    public void postStop() {
	  cluster.unsubscribe(getSelf());
    }

	/**
	 * {@inheritDoc}
	 *
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Test and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 * @throws NullPointerException
	 * @throws IOException
	 * @throws NoSuchElementException
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CrawlActionMessage.class, crawl_action-> {
					Map<String, Boolean> frontier = new HashMap<>();
					Map<String, Page> visited = new HashMap<>();
					Domain domain = crawl_action.getDomain();
					
					String initial_url = "http://"+domain.getHost()+"/"+domain.getEntryPath();
					
					//add link to frontier
					frontier.put(initial_url, Boolean.TRUE);
					
					
					while(!frontier.isEmpty()) {
						
						Map<String, Boolean> external_links = new HashMap<>();
						//remove link from beginning of frontier
						String page_url_str = frontier.keySet().iterator().next();
						log.warn("page url string :: "+page_url_str);
						log.warn("frontier size before remove : :  "+frontier.size());
						//page_url_str = BrowserUtils.sanitizeUserUrl(page_url_str);
						//log.warn("page url string after sanitize  ::  "+page_url_str);
						frontier.remove(page_url_str);
						log.warn("frontier size after remove ::  "+frontier.size());
						if( BrowserUtils.isImageUrl(page_url_str) || page_url_str.endsWith(".pdf")){
							continue;
						}		
						
						URL page_url = new URL(page_url_str);
						String path = page_url.getPath();
						

						//construct page and add page to list of page states
						

						//retrieve html source for page
						try {
							Document doc = Jsoup.connect(page_url_str).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get();
							
							Page page = new Page(new ArrayList<>(), Browser.cleanSrc(doc.outerHtml()), doc.title(), (page_url.getHost()+path), path);
							page = page_service.save( page );

							domain.addPage(page);
							domain = domain_service.save(domain);
							
							visited.put(page_url_str, page);
							//send message to page data extractor
							log.debug("sending page to an audit manager...");
							getSender().tell(page, getSelf());
							log.warn("page url :: "+page_url);
							log.warn("page host :: "+page_url.getHost());
							log.warn("page path :: "+page_url.getPath());
							log.warn("----------------------------------------------------------------");
							log.warn("----------------------------------------------------------------");
							
							Elements links = doc.select("a");
							for (Element link : links) {
								
								String href_str = link.absUrl("href");
								if(href_str == null || href_str.isEmpty()) {
									continue;
								}
								
								String href = BrowserUtils.sanitizeUserUrl(href_str);
								//check if external link
								if( BrowserUtils.isExternalLink(domain.getHost().replace("www.", ""), href_str) || href_str.startsWith("mailto:")) {
									log.debug("adding to external links :: "+href_str);
				   					external_links.put(href_str, Boolean.TRUE);
								}
								else if( !visited.containsKey(href) && !frontier.containsKey(href)){
									log.warn("href after sanitize :: "+href_str);
									log.warn("adding link to frontier :: "+href);
									//add link to frontier
									frontier.put(href, Boolean.TRUE);
								}
							}
						}catch(SocketTimeoutException e) {
							log.warn("Error occurred while navigating to :: "+page_url_str);
						}
						catch(HttpStatusException e) {
							log.warn("HTTP Status Exception occurred while navigating to :: "+page_url_str);
							e.printStackTrace();
						}
						catch(IllegalArgumentException e) {
							log.warn("illegal argument exception occurred when connecting to ::  " + page_url_str);
							e.printStackTrace();
						}
						catch(UnsupportedMimeTypeException e) {
							log.warn(e.getMessage() + " : " +e.getUrl());
						}
					}
					System.out.println("total links visited :::  "+visited.keySet().size());
				})
				.match(PageState.class, page_state -> {
					log.warn("Web crawler received page state");
					Page page = page_state_service.getParentPage(page_state.getKey());
					
					//send Journey to JourneyExplorer actor
					ActorRef journeyMapper = actor_system.actorOf(SpringExtProvider.get(actor_system)
							.props("journeyMappingManager"), "journeyMappingManager"+UUID.randomUUID());
					journeyMapper.tell(new URL(page.getUrl()), getSelf());
					
					boolean rendering_not_complete = true;
					int cnt = 0;
					List<String> element_xpaths_reviewed = new ArrayList<>();
					do {
						try {
							log.warn("getting browser for rendered page state extraction...");
							//navigate to page url
							Browser browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
							log.warn("navigating to page state url ::   "+page.getUrl());
							browser.navigateTo(page.getUrl());
							
							//URL url = new URL(page_state.getUrl());
							
							//extract full page screenshot
							/* Uncomment when ready to convert to rendered page and rendered element setup
					BufferedImage full_page_screenshot = browser.getFullPageScreenshot();		
					String full_page_screenshot_checksum = PageState.getFileChecksum(full_page_screenshot);
					String full_page_screenshot_url = UploadObjectSingleOperation.saveImageToS3(full_page_screenshot, url.getHost(), full_page_screenshot_checksum, BrowserType.create(browser.getBrowserName()));
					full_page_screenshot.flush();
							 */

							//extract Element screenshots
							List<ElementState> elements = page_state.getElements();// browser_service.extractElementStates(page_state.getSrc(), url);
							log.warn("elements loaded for page state :: " +page_state.getElements().size());
							for(ElementState element_state : elements) {
								if(element_xpaths_reviewed.contains(element_state.getXpath())) {
									continue;
								}
//								long start_time = System.currentTimeMillis();
								WebElement web_element = browser.getDriver().findElement(By.xpath(element_state.getXpath()));
								Map<String, String> css_props = Browser.loadCssProperties(web_element, browser.getDriver());
								element_state.setRenderedCssValues(css_props);
								element_state.setVisible(web_element.isDisplayed());
								element_state_service.save(element_state);
								element_xpaths_reviewed.add(element_state.getXpath());
								break;
					
//								long end_time = System.currentTimeMillis();
							}
							
							//get rendered css values for element
							//get screenshot of element
							//extract browser offsets 
							long x_offset = browser.getXScrollOffset();
							long y_offset = browser.getYScrollOffset();
							
							//extract browser dimensions
							Dimension dimension = browser.getViewportSize();
							
							//Save data to rendered state with dimensions. use element states and dimensions for key generation
							
							//send RenderedPageState to sender
							log.warn("telling sender of Rendered Page State outcomes ....");
							getSender().tell(new RenderedPageState(page_state), getSelf());
							rendering_not_complete = false;
							break;
						}catch(WebDriverException e) {
							log.warn("Webdriver exception thrown..."+e.getMessage());
							e.printStackTrace();
						}
						catch(GridException e) {
							log.warn("Grid exception thrown ...  ");
							e.printStackTrace();
						}
					}while(rendering_not_complete && cnt < 20);
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.info("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.info("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.info("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}
}
