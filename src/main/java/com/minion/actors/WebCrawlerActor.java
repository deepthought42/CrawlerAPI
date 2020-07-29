package com.minion.actors;


import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
import com.qanairy.utils.BrowserUtils;

import akka.actor.AbstractActor;
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
	private DomainService domain_service;
	
	@Autowired
	private PageService page_service;
	
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
					
					String initial_url = domain.getProtocol()+"://"+domain.getHost()+"/"+domain.getEntryPath();
					//add link to frontier
					frontier.put(initial_url, Boolean.TRUE);
					
					
					while(!frontier.isEmpty()) {
						
						Map<String, Boolean> external_links = new HashMap<>();
						Page page = null;
						//remove link from beginning of frontier
						String page_url = frontier.keySet().iterator().next();
						frontier.remove(page_url);
						if(page_url.isEmpty() || page_url.contains("mailto:") || page_url.contains(".jpg") || page_url.contains(".png")) {
							continue;
						}

						if(page_url.endsWith(".pdf")){
							continue;
						}
		
						//construct page and add page to list of page states
						page = new Page(page_url);
						page = page_service.save( page );

						domain.addPage(page);
						domain = domain_service.save(domain);
						
						visited.put(page_url, page);
						//send message to page data extractor
						log.debug("sending page to an audit manager...");
						getSender().tell(page, getSelf());

						//retrieve html source for page
						try {
							Document doc = Jsoup.connect(page_url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get();
							Elements links = doc.select("a");
							for (Element link : links) {
								String href = BrowserUtils.sanitizeUrl(link.absUrl("href"));
								
								//check if external link
								if( !href.isEmpty() && (BrowserUtils.isExternalLink(domain.getHost().replace("www.", ""), href) || href.startsWith("mailto:"))) {
									log.debug("adding to external links :: "+href);
				   					external_links.put(href, Boolean.TRUE);
								}
								else if( !href.isEmpty() && !visited.containsKey(href) && !frontier.containsKey(href) && !BrowserUtils.isImageUrl(href)){
									log.warn("adding link to frontier :: "+href);
									//add link to frontier
									frontier.put(href, Boolean.TRUE);
								}
							}
						}catch(SocketTimeoutException e) {
							log.warn("Error occurred while navigating to :: "+page_url);
						}
						catch(HttpStatusException e) {
							log.warn("HTTP Status Exception occurred while navigating to :: "+page_url);
							e.printStackTrace();
						}
						catch(IllegalArgumentException e) {
							log.warn("illegal argument exception occurred when connecting to ::  " + page_url);
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
					
					boolean rendering_not_complete = true;
					int cnt = 0;
					
					do {
						try {
							log.warn("getting browser for rendered page state extraction...");
							//navigate to page url
							Browser browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
							log.warn("navigating to page state url ::   "+page_state.getUrl());
							browser.navigateTo(page_state.getUrl());
							
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
							for(ElementState element : elements) {
//								long start_time = System.currentTimeMillis();
								try {
									WebElement web_element = browser.getDriver().findElement(By.xpath(element.getXpath()));
									Map<String, String> css_props = Browser.loadCssProperties(web_element, browser.getDriver());
									element.setRenderedCssValues(css_props);
									element_state_service.save(element);
								}catch(WebDriverException e) {
									log.warn("no such element exception thrown for element with xpath :: "+element.getXpath()+"    :   on page     :    "+page_state.getUrl());
									//log.warn(e.getMessage());
									//e.printStackTrace();
								}
//								long end_time = System.currentTimeMillis();
//								log.warn("total time to load rendered css properties ::   "+((end_time-start_time)));
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
							log.warn("Webdriver exception thrown...");
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
