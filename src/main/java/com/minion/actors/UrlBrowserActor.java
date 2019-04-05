package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AbstractActor;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.minion.structs.Message;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.message.PageStateMessage;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PageElementState;
import com.qanairy.models.PageState;
import com.qanairy.services.BrowserService;
import com.qanairy.services.PageStateService;
import com.qanairy.services.TestCreatorService;
import com.qanairy.services.TestService;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
@Component
@Scope("prototype")
public class UrlBrowserActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(UrlBrowserActor.class.getName());
	
	@Autowired
	DiscoveryRecordRepository discovery_repo;
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private TestCreatorService test_creator_service;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message -> {
					if(message.getData() instanceof URL){
						
						String discovery_key = message.getOptions().get("discovery_key").toString();
						
						String url = ((URL)message.getData()).toString();
						String host = ((URL)message.getData()).getHost();
						String browser_name = message.getOptions().get("browser").toString();

						List<PageState> page_states = buildPageStates(url, browser_name, host, message);
/*
						System.err.println("###############################################################");
						System.err.println("Page states returned ....  "+page_states.size());
						System.err.println("###############################################################");
					
						System.err.println("###############################################################");
						System.err.println("Page states 0 elements ....  "+page_states.get(0).getElements().size());
						System.err.println("###############################################################");
					
						Test test = test_creator_service.createLandingPageTest(page_states.get(0), browser_name);
						test = test_service.save(test, host);

						System.err.println("Page states 0 elements ....  "+test.getResult().getElements().size());

						
						Message<Test> test_msg = new Message<Test>(message.getAccountKey(), test, message.getOptions());
						*/
						/**  path expansion temporarily disabled
						 */
						/*
						final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
						path_expansion_actor.tell(test_msg, getSelf() );
						MessageBroadcaster.broadcastDiscoveredTest(test, host);

						DiscoveryRecord discovery_record = discovery_repo.findByKey( discovery_key);

						for(PageState page_state : page_states.subList(1, page_states.size())){
							
							System.err.println("###############################################################");
							System.err.println("Page states elements ....  "+page_state.getElements().size());
							System.err.println("###############################################################");
							
							log.warn("Discovery record :: " + discovery_record);
							log.warn("test :: " + test);
							log.warn("test result " + test.getResult());
							log.warn("test result elements size   ::  " + page_state.getElements().size());
							if(!discovery_record.getExpandedPageStates().contains(test.getResult().getKey())){
								log.warn("discovery path does not have expanded page state");
								discovery_record.addExpandedPageState(test.getResult().getKey());
								PageStateMessage page_state_msg = new PageStateMessage(message.getAccountKey(), page_state, discovery_record, message.getOptions());

								final ActorRef form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("formDiscoveryActor"), "form_discovery"+UUID.randomUUID());
								form_discoverer.tell(page_state_msg, getSelf() );
									
								final ActorRef path_expansion_actor_2 = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
								path_expansion_actor_2.tell(page_state_msg, getSelf() );
							}
							discovery_record.setLastPathRanAt(new Date());
							discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
							
							discovery_record.setTestCount(discovery_record.getTestCount()+1);
							discovery_record = discovery_repo.save(discovery_record);
							MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
						}
						*/
						
						
						/*
						do{
							try{
								browser = browser_service.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
								test = test_creator_service.generateLandingPageTest(url, browser);
								System.err.println("###############################################################");
								System.err.println("host value when saving page load test :: "+host);
								System.err.println("###############################################################");
								test = test_service.save(test, host);
							}
							catch(Exception e){
								log.error("Exception occurred while exploring url --  " + e.getMessage());
							}
							finally{
								if(browser!=null){
									browser.close();
								}
							}							
						}while(test==null);
						*/
						
						
				   }
					//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);
					postStop();

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
					log.info("received unknown message");
				})
				.build();
	}
	
	

	public List<PageState> buildPageStates(String url, String browser_name, String host, Message<?> msg){
		List<PageState> page_states = new ArrayList<>();
		boolean error_occurred = false;		
		String discovery_key = msg.getOptions().get("discovery_key").toString();

		Browser browser = null;
		do{
			try{
				browser = BrowserConnectionFactory.getConnection(browser_name, BrowserEnvironment.DISCOVERY);
				browser.navigateTo(url);
				//get current viewport screenshot
				String browser_url = browser.getDriver().getCurrentUrl();
				System.err.println("starter current url :: "+browser_url);
				URL page_url = new URL(browser_url);
		        
				int param_index = page_url.toString().indexOf("?");
				String url_without_params = page_url.toString();
				if(param_index >= 0){
					url_without_params = url_without_params.substring(0, param_index);
				}
				
				List<WebElement> web_elements = browser.getDriver().findElements(By.cssSelector("*"));
				System.err.println("web elements at start :: " + web_elements.size());

				web_elements = BrowserService.fitlerNonDisplayedElements(web_elements);
				web_elements = BrowserService.filterStructureTags(web_elements);
				web_elements = BrowserService.filterNoWidthOrHeight(web_elements);
				web_elements = BrowserService.filterNonChildElements(web_elements);
				web_elements = BrowserService.filterElementsWithNegativePositions(web_elements);

				int iter_idx=-1;
				while(!web_elements.isEmpty()){
					iter_idx++;
					log.warn("identifying page state iteration ...."+iter_idx+".... elements remaining ...."+web_elements.size());
					if(iter_idx != 0){
						log.warn("element not visible in viewport. SCROLLING TO ELEMENT the scrolling to offset for continuity of screenshots");
						browser.scrollToElement(web_elements.get(0));
					}
										
					BufferedImage viewport_screenshot = Browser.getViewportScreenshot(browser.getDriver());		
					String page_key = "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(url_without_params+ PageState.getFileChecksum(viewport_screenshot));
					
					PageState page_state = page_state_service.findByKey(page_key);
					if(page_state != null){
						System.err.println("page already exists. Loading elements and screenshot ::  "+page_key);
						//page_state.setElements(page_state_service.getPageElementStates(page_key));
						//page_state.setBrowserScreenshots(page_state_service.getScreenshots(page_key));
						page_states.add(page_state);
					}
					else {
						page_state = browser_service.buildPage(browser);
						page_states.add(page_state);
					}
					
					List<WebElement> element_list = new ArrayList<>();
					//remove elements from page web elements list
					System.err.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					System.err.println("Browser offset :: "+browser.getXScrollOffset()+","+browser.getYScrollOffset() );
					System.err.println("Browser dimension :: " +browser.getViewportSize().getWidth()+","+browser.getViewportSize().getHeight());
					System.err.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					
					//convert page elements to map with coordinates of element as key
					Map<String,PageElementState> element_map = new HashMap<String, PageElementState>();
					for(PageElementState page_elem : page_state.getElements()){
						element_map.put(page_elem.getXLocation()+""+page_elem.getYLocation(), page_elem);
					}

					for(WebElement elem: web_elements){
						if(!element_map.containsKey(elem.getLocation().getX()+""+elem.getLocation().getY())){
							element_list.add(elem);
						}
					}
					System.err.println("page elements key map size :: " + element_map.keySet().size());
					System.err.println("page elements size :: " + page_state.getElements().size());
					System.err.println("previous web elements size :: " + web_elements.size());
					System.err.println("new web element list size ::  " + element_list.size());
					System.err.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");

					web_elements = element_list;
				
					
					
					DiscoveryRecord discovery_record = discovery_repo.findByKey( discovery_key);
					System.err.println("###############################################################");
					System.err.println("Page states elements ....  "+page_state.getElements().size());
					System.err.println("###############################################################");
					
					log.warn("Discovery record :: " + discovery_record);
					log.warn("test result " + page_state);
					log.warn("test result elements size   ::  " + page_state.getElements().size());
					if(!discovery_record.getExpandedPageStates().contains(page_state.getKey())){
						discovery_record.addExpandedPageState(page_state.getKey());

						if(iter_idx == 0){
							log.warn("creating page state test");

							Test test = test_creator_service.createLandingPageTest(page_states.get(0), browser_name);
							test = test_service.save(test, host);
							
							Message<Test> test_msg = new Message<Test>(msg.getAccountKey(), test, msg.getOptions());

							/**  path expansion temporarily disabled
							 */
							
							final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
							path_expansion_actor.tell(test_msg, getSelf() );
							
						}
						else{
							log.warn("discovery path does not have expanded page state");
							PageStateMessage page_state_msg = new PageStateMessage(msg.getAccountKey(), page_state, discovery_record, msg.getOptions());
	
							final ActorRef form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("formDiscoveryActor"), "form_discovery"+UUID.randomUUID());
							form_discoverer.tell(page_state_msg, getSelf() );
								
							final ActorRef path_expansion_actor_2 = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
							path_expansion_actor_2.tell(page_state_msg, getSelf() );
						}
					}
					discovery_record.setLastPathRanAt(new Date());
					discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
					
					discovery_record.setTestCount(discovery_record.getTestCount()+1);
					discovery_record = discovery_repo.save(discovery_record);
					MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

					
					if(iter_idx > 3){
						for(WebElement elem : web_elements){
							System.err.println("element tag name :: " + elem.getTagName() + ";      "+elem.getText());
							System.err.println("element location  :::   "+elem.getLocation().getX()+","+elem.getLocation().getY());
							System.err.println("element location  :::   "+elem.getSize().getWidth()+","+elem.getSize().getHeight());
						}
					}
				}
				error_occurred = false;
				break;
			}catch(NullPointerException e){
				log.warn("Error happened while exploratory actor attempted to crawl test "+e.getMessage());
				e.printStackTrace();
				error_occurred = true;
			} catch (GridException e) {
				log.warn("Grid exception encountered while trying to crawl exporatory path"+e.getMessage());
				error_occurred = true;
			}
			catch (NoSuchElementException e){
				log.error("Unable to locage element while performing path crawl   ::    "+ e.getMessage());
				e.printStackTrace();
				error_occurred = true;
			}
			catch (WebDriverException e) {
				//TODO: HANDLE EXCEPTION THAT OCCURS BECAUSE THE PAGE ELEMENT IS NOT ON THE PAGE
				log.warn("WebDriver exception encountered while trying to crawl exporatory path"+e.getMessage());
				error_occurred = true;
				//e.printStackTrace();
			} catch(Exception e){
				log.warn("Exception occurred in getting page states. \n"+e.getMessage());
				e.printStackTrace();
				error_occurred = true;
			}
			finally{
				if(browser != null){
					browser.close();
				}
			}
		}while(error_occurred);
		
		return page_states;
	}

}