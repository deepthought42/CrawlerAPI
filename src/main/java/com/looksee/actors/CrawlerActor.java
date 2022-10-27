package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.api.MessageBroadcaster;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.Form;
import com.looksee.models.PageState;
import com.looksee.models.TestUser;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.FormType;
import com.looksee.models.enums.PathStatus;
import com.looksee.models.enums.SubscriptionPlan;
import com.looksee.models.journeys.LoginStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.BrowserCrawlActionMessage;
import com.looksee.models.message.ConfirmedJourneyMessage;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.DiscardedJourneyMessage;
import com.looksee.models.message.JourneyExaminationProgressMessage;
import com.looksee.models.message.JourneyMessage;
import com.looksee.models.message.Message;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;
import com.looksee.services.SubscriptionService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.JourneyUtils;
import com.looksee.utils.ListUtils;
import com.looksee.utils.PageUtils;
import com.looksee.utils.PathUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class CrawlerActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(CrawlerActor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private SubscriptionService subscription_service;

	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	private Account account;
	private Domain domain;
	private ActorRef audit_manager;
	
	Map<String, Boolean> visited_urls = new HashMap<>();
	//Map<String, Boolean> page_urls = new HashMap<>();
	
	private Map<String, List<ElementState>> explored_elements = new HashMap<>();
	private Map<Integer, String> reviewed_journeys = new HashMap<>();
	//PROGRESS TRACKING VARIABLES
	private int generated_journeys = 0;
	
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
				.match(CrawlActionMessage.class, message -> {
					audit_manager = getContext().getSender();
					if(CrawlAction.START.equals(message.getAction())) {
						log.warn("STARTING crawl actor. Sending message to page state builder");
						
						ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
								  .props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
						page_state_builder.tell(message, getSelf());
					}
					else if(CrawlAction.STOP.equals(message.getAction())) {
						
					}
				})
				.match(PageDataExtractionError.class, msg -> {
					String page_url = BrowserUtils.getPageUrl(msg.getUrl());
					visited_urls.put(page_url, Boolean.TRUE);
				})
				.match(PageDataExtractionMessage.class, msg -> {
					String page_url = BrowserUtils.getPageUrl(msg.getPageState().getUrl());

					visited_urls.put(page_url, Boolean.TRUE);
					log.warn("page data extraction message received..."+page_url);

					this.audit_manager.tell(msg, getSelf());
					
					if(this.account == null) {
						this.account = account_service.findById(msg.getAccountId()).get();
					}
					if(this.domain == null) {
						domain = domain_service.findById(msg.getDomainId()).get();
					}
					SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());

					if(subscription_service.hasExceededDomainPageAuditLimit(plan, visited_urls.size())) {
						log.warn("Account has exceeded domain page audit limit for subscription..."+visited_urls.size());
						
						//send message to audit manager that subscription was exceeded
						//NOTE: Make sure audit manager broadcasts message to user indicating they have exceeded subscription
						
						AuditRecord record = audit_record_service.findById(msg.getAuditRecordId()).get();
						record.setStatus(ExecutionStatus.EXCEEDED_SUBSCRIPTION);
						audit_record_service.save(record, msg.getAccountId(), msg.getDomainId());
												
						MessageBroadcaster.broadcastSubscriptionExceeded(this.account);
					}
					else {
						log.warn("CrawlerActor received PageDataExtraction Message");
	
						//generate form steps						
						Set<Form> forms = PageUtils.extractAllForms(msg.getPageState());
						Set<Form> unexplored_forms = new HashSet<>();
						
						for(Form form: forms) {
							if(!isElementExplored(msg.getPageState().getUrl(), form.getFormTag())) {
								unexplored_forms.add(form);
							}
							addElementsToExploredMap(msg.getPageState(), form.getFormTag());
						}
						
						//generate form journeys
						List<Step> form_steps = generateFormSteps(msg.getDomainId(), msg.getPageState(), unexplored_forms);
						log.warn(form_steps.size() + " FORM STEPS identified");

						for(Step step: form_steps) {
							List<Step> step_list = new ArrayList<>();
							step_list.add(step);
							generated_journeys++;
							reviewed_journeys.put(generated_journeys, null);
							sendJourneyMessages(generated_journeys, 
												step_list, 
												PathStatus.EXPANDED, 
												BrowserType.CHROME, 
												msg,
												"journeyExecutor");
						}
						
						//generate journey steps for interactive non link elements
						List<List<Step>> steps = generateJourneys(msg.getPageState());
						
						for(List<Step> step_list : steps) {
							generated_journeys++;
							reviewed_journeys.put(generated_journeys, null);
							sendJourneyMessages(generated_journeys, 
												step_list, 
												PathStatus.EXPANDED, 
												BrowserType.CHROME, 
												msg,
												"journeyExecutor");
						}
						
						//EXTRACT LINKS AND SEND NON EXTERNAL AND NON VISITED LINKS AND LINKS THAT AREN'T YET ON THE FRONTIER TO BE BUILT
						log.warn("page state :: "+msg.getPageState());
						log.warn("domain url :: "+domain.getUrl());
						List<URL> links = extractLinks(msg.getPageState(), domain.getUrl());
						//send links to page state builder
						sendLinksToPageStateBuilder(links, visited_urls, msg);

						
						
						//send generated and examined journey counts to audit manager
						int examined_journeys = getExaminedJourneyCount(reviewed_journeys)+getExaminedPages(visited_urls);
						int total_journeys = reviewed_journeys.size() + visited_urls.size();
						
						log.warn("# examined journeys (PD) :: "+getExaminedJourneyCount(reviewed_journeys)+ " : " +getExaminedPages(visited_urls) + " : " + examined_journeys);
						log.warn("# generated journeys (PD) :: "+ reviewed_journeys.size() + " : " + visited_urls.size() + " : "+total_journeys);
						log.warn("visited urls : "+visited_urls);
						JourneyExaminationProgressMessage progress_msg = new JourneyExaminationProgressMessage(msg.getAccountId(), 
																											   msg.getAuditRecordId(), 
																											   msg.getDomainId(),
																											   examined_journeys,
																											   total_journeys);
						audit_manager.tell(progress_msg, getSelf());
					}
				})
				.match(BrowserCrawlActionMessage.class, message -> {
					ActorRef page_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
														.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
					page_builder.tell(message, getSelf());
				})
				.match(DiscardedJourneyMessage.class, message -> {
					log.warn("received discarded journey :: "+message.getId());
					reviewed_journeys.put(message.getId(), "DISCARDED");
					log.warn("journeys reviewed :: "+reviewed_journeys);

					int examined_journeys = getExaminedJourneyCount(reviewed_journeys)+getExaminedPages(visited_urls);
					int total_journeys = reviewed_journeys.size() + visited_urls.size();
					//send generated and examined journey counts to audit manager
					log.warn("# examined journeys(discarded) :: "+getExaminedJourneyCount(reviewed_journeys)+ " : " +getExaminedPages(visited_urls) + " : " +examined_journeys);
					log.warn("# generated journeys(discarded) :: "+ reviewed_journeys.size() + " : " + visited_urls.size() + " : "+total_journeys);
					JourneyExaminationProgressMessage progress_msg = new JourneyExaminationProgressMessage(message.getAccountId(), 
																										   message.getAuditRecordId(), 
																										   message.getDomainId(),
																										   examined_journeys,
																										   total_journeys);
					audit_manager.tell(progress_msg, getSelf());
				})
				.match(ConfirmedJourneyMessage.class, message -> {
					try { 
						log.warn("received confirmed journey :: "+message.getId());						
						reviewed_journeys.put(message.getId(), "CONFIRMED");
	
						log.warn("crawler received confirmed journey message steps :: "+message.getSteps());
						if(this.account == null) {
							this.account = account_service.findById(message.getAccountId()).get();
						}
						SubscriptionPlan plan = SubscriptionPlan.create(this.account.getSubscriptionType());
						
						PageState final_page = PathUtils.getLastPageState(message.getSteps());
	
						//load in element states
						//final_page.setElements(page_state_service.getElementStates(final_page.getId()));
						
						if(this.domain == null) {
							domain = domain_service.findById(message.getDomainId()).get();
						}
						
						audit_manager.tell(message.clone(), getSelf());
						
						if( BrowserUtils.isExternalLink(domain.getUrl(), final_page.getUrl()) 
								|| visited_urls.containsKey(final_page.getUrl())) 
						{
							log.warn("Identified external or visited link "+final_page.getUrl());
						}
						else {
							//audit_record_service.addPageToAuditRecord(message.getAuditRecordId(), final_page.getId());
							List<ElementState> element_states = savePageAndElements(final_page);
							final_page.setElements(element_states);
							audit_record_service.addPageToAuditRecord(message.getAuditRecordId(), final_page.getId());
	
	
							//Add page state to visited list
							if(subscription_service.hasExceededDomainPageAuditLimit(plan, visited_urls.size())) {
								log.warn("account has exceeded subscription plan");
							}
							else {
								visited_urls.put(final_page.getUrl(), Boolean.TRUE);
								Set<Form> forms = PageUtils.extractAllForms(final_page);
								Set<Form> unexplored_forms = new HashSet<>();
								
								for(Form form: forms) {
									if(!isElementExplored(final_page.getUrl(), form.getFormTag())) {
										unexplored_forms.add(form);
									}
									addElementsToExploredMap(final_page, form.getFormTag());
								}
								
								//generate form journeys
								List<Step> steps = generateFormSteps(message.getDomainId(), final_page, unexplored_forms);
								for(Step step: steps) {
									List<Step> steps_list = JourneyUtils.trimPreLoginSteps(message.getSteps());
									
									//if step start page matches another start page url in the step list then discard
									if(steps_list.contains(step) || JourneyUtils.hasStartPageAlreadyBeenExpanded(steps_list, step)) {
										log.warn("FORM STEP EXISTS IN STEP LIST ALREADY");
									}
									else {
										steps_list.add(step);
										generated_journeys++;
										reviewed_journeys.put(generated_journeys, null);
										sendJourneyMessages(generated_journeys, 
															steps_list, 
															PathStatus.EXPANDED, 
															BrowserType.CHROME, 
															message,
															"journeyExecutor");
									}
								}
	
								//generate journey steps for interactive non link elements
								List<List<Step>> new_steps = generateJourneys(final_page);
								
								for(List<Step> step_list : new_steps) {
									generated_journeys++;
									reviewed_journeys.put(generated_journeys, null);
									sendJourneyMessages(generated_journeys, 
														step_list, 
														PathStatus.EXPANDED, 
														BrowserType.CHROME, 
														message,
														"journeyExecutor");
								}
								
								//EXTRACT LINKS AND SEND NON EXTERNAL AND NON VISITED LINKS AND LINKS THAT AREN'T YET ON THE FRONTIER TO BE BUILT
								List<URL> links = extractLinks(final_page, domain.getUrl());
								//send links to page state builder
								sendLinksToPageStateBuilder(links, visited_urls, message);
								
							}						
						}
						
						//send generated and examined journey counts to audit manager
						int examined_journeys = getExaminedJourneyCount(reviewed_journeys)+getExaminedPages(visited_urls);
						int total_journeys = reviewed_journeys.size() + visited_urls.size();
						
						log.warn("# examined journeys (CJ) :: "+getExaminedJourneyCount(reviewed_journeys)+ " : " +getExaminedPages(visited_urls) + " : " +examined_journeys);
						log.warn("# generated journeys (CJ) :: "+ reviewed_journeys.size() + " : " + visited_urls.size() + " : "+total_journeys);
						
						JourneyExaminationProgressMessage progress_msg = new JourneyExaminationProgressMessage(message.getAccountId(), 
																											message.getAuditRecordId(), 
																											message.getDomainId(),
																											examined_journeys,
																											total_journeys);
						audit_manager.tell(progress_msg, getSelf());
					}catch(Exception e) {
						e.printStackTrace();
					}
				})
				.match(MemberUp.class, mUp -> {
					log.debug("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.debug("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.debug("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.debug("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}
	
	private int getExaminedPages(Map<String, Boolean> page_urls) {
		int count = 0;
		for(Boolean val : page_urls.values()) {
			if(Boolean.TRUE.equals(val)) {
				count++;
			}
		}
		return count;
	}

	private void sendJourneyMessages(int journey_id, 
									 List<Step> step_list, 
									 PathStatus path_status,
									 BrowserType browser, 
									 Message msg, 
									 String actorName) 
	{	
		JourneyMessage journey_msg = new JourneyMessage(journey_id, 
														ListUtils.clone(step_list), 
														path_status, 
														browser, 
														msg.getDomainId(),
														msg.getAccountId(), 
														msg.getAuditRecordId());
		ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
												.props(actorName), actorName+UUID.randomUUID());
		journey_executor.tell(journey_msg, getSelf());
		
	}

	/**
	 * Sends links to page state builder actor
	 * @param links
	 * @param page_urls
	 * @param message
	 */
	private void sendLinksToPageStateBuilder(List<URL> links, Map<String, Boolean> page_urls, Message message) {
		for(URL link : links) {
			String link_page_url = BrowserUtils.getPageUrl(link);
			if(page_urls.containsKey(link_page_url)) {
				continue;
			}
			page_urls.put(link_page_url, Boolean.FALSE);
			
			CrawlActionMessage crawl_message = new CrawlActionMessage(CrawlAction.START, 
																message.getDomainId(), 
																message.getAccountId(),
																message.getAuditRecordId(),
																true, 
																link, 
																domain.getUrl());
			
			ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
					  .props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
			page_state_builder.tell(crawl_message, getSelf());
		}
	}

	/**
	 * Extracts link href values from {@link ElementState} that belong to {@link PageState}
	 * 
	 * @param pageState
	 * @param domain_host host value for the parent domain
	 * 
	 * @return {@link List} of link href values
	 * 
	 * @pre pageState != null
	 */
	private List<URL> extractLinks(PageState pageState, String domain_host) {
		assert pageState != null;
		
		List<ElementState> filtered_elements = pageState.getElements()
														.parallelStream()
														.filter(element -> element.isVisible())
														.filter(element -> {
																Dimension dimension = new Dimension(element.getWidth(), element.getHeight()); 
																Point location = new Point(element.getXLocation(), element.getYLocation());
																return BrowserService.hasWidthAndHeight(dimension) && !BrowserService.doesElementHaveNegativePosition(location);
														})
														.filter(element -> !BrowserService.isStructureTag(element.getName()))
														.filter(element -> isLinkElement(element))
														.collect(Collectors.toList());
		
		List<URL> hrefs = new ArrayList<>();
		
		for(ElementState link: filtered_elements) {
			String href_str = link.getAttribute("href");
			
			if(!BrowserUtils.isValidLink(href_str)) {
				log.warn(href_str+" is invalid!");
				continue;
			}
			
			try {
				href_str = href_str.replaceAll(";", "")
								   .replace("[", "")
								   .replace("]", "")
								   .trim();
				String formatted_url = BrowserUtils.formatUrl("http", domain.getUrl(), href_str, false);
				String sanitized_url = BrowserUtils.sanitizeUrl(formatted_url, false);
				URL href_url = new URL(sanitized_url);
				
				if(BrowserUtils.isExternalLink(domain_host, href_url.toString())) {
					continue;
				}
				else {
					hrefs.add(href_url);
				}
			}
			catch(MalformedURLException e) {
				log.error("malformed href value ....  " + href_str);
			}

		}
		
		return hrefs;
	}

	/**
	 * Generates list consisting of lists of steps for interactive elements that aren't links
	 * @param pageState
	 * @return
	 */
	private List<List<Step>> generateJourneys(PageState pageState) {		
		List<ElementState> filtered_elements = pageState.getElements().parallelStream()
														.filter(element -> element.isVisible())
														.filter(element -> {
																Dimension dimension = new Dimension(element.getWidth(), element.getHeight()); 
																Point location = new Point(element.getXLocation(), element.getYLocation());
																return BrowserService.hasWidthAndHeight(dimension) && !BrowserService.doesElementHaveNegativePosition(location);
														})
														.filter(element -> !BrowserService.isStructureTag(element.getName()))
														.filter(element -> isInteractiveElement(element))
														.filter(element -> !isLinkElement(element))
														.filter(element -> !isElementExplored(pageState.getUrl(), element))
														.filter(element -> !isInputElement(element))
														.collect(Collectors.toList());
		
		//addElementsToExploredMap(pageState, filtered_elements);

		List<Step> new_steps = generateSteps(pageState, filtered_elements);

		List<List<Step>> journeys = new ArrayList<>();
		//generate new journeys with new steps and send to journey executor to be evaluated
		for(Step step: new_steps) {
			List<Step> steps = new ArrayList<>();
			steps.add(step);
			
			journeys.add(steps);
		}
		
		return journeys;
	}

	private int getExaminedJourneyCount(Map<Integer, String> reviewed_journeys) {
		int journeys_examined = 0;
		for(int key: reviewed_journeys.keySet()) {
			if(reviewed_journeys.get(key) != null) {
				journeys_examined++;
			}
		}
		
		return journeys_examined;
	}

	/**
	 * 
	 * @param page_state
	 * @throws Exception 
	 */
	private List<ElementState> savePageAndElements(PageState page_state) throws Exception {
		List<ElementState> element_states = element_state_service.saveAll(page_state.getElements(), page_state.getId());
		//page_state.setElements(element_states);
		//page_state = page_state_service.save(page_state);

		List<Long> element_ids = element_states.parallelStream().map(element -> element.getId()).collect(Collectors.toList());
		page_state_service.addAllElements(page_state.getId(), element_ids);
		return element_states;
	}

	private boolean isInputElement(ElementState element) {
		return element.getName().contentEquals("input");
	}
	
	private boolean isLinkElement(ElementState element) {
		return element.getName().contentEquals("a");
	}

	private List<Step> generateFormSteps(long domain_id, PageState pageState, Set<Form> forms) {
		List<Step> steps = new ArrayList<>();
		for(Form form: forms) {
			
			if(FormType.LOGIN.equals(form.getType())){
				//retrieve user credentials
				List<TestUser> user_list = domain_service.findTestUsers(domain_id);
				if(!user_list.isEmpty()) {
					
					List<String> username_input_types = new ArrayList<>();
					username_input_types.add("username");
					username_input_types.add("email");
					//get username field
					ElementState username_element = getFormField(form.getFormFields(), username_input_types);
				
					//get password field
					ElementState password_element = getFormField(form.getFormFields(), "password");

					//get submit button
					ElementState submit_btn = form.getSubmitField();
					
					log.warn("LOGIN FORM FOUND. URL = "+pageState.getUrl());
					log.warn("username element :: "+username_element);
					log.warn("password element :: "+password_element);
					log.warn("Submit button :: "+submit_btn);
					//create step to click on submit button
					steps.add( new LoginStep(pageState, null, username_element, password_element, submit_btn, user_list.get(0)) );
				}
				else {
					log.warn("THROW ERROR HERE WARNING USER OF LACK OF TEST USER CONFIGURED");
				}
			}
			else if(FormType.REGISTRATION.equals(form.getType())) {
				//generate username value
				//generate password value
				//save user credentials
				
				//get username field
				//create step to enter username into username field
				
				//get password field
				//create step to enter password into password field
				
				//get password confirmation field if it exists
				//create step to enter password into password confirmation field
				
				//get submit button
				//create step to click on submit button
			}
		}
		return steps;
	}

	private ElementState getFormField(List<ElementState> formFields, String form_string) {
		for(ElementState element: formFields) {
			for(String attr_value : element.getAttributes().values()) {
				if(attr_value.toLowerCase().contains(form_string)) {
					return element;
				}
			}
		}
		return null;
	}
	
	/**
	 * Checks form fields for any input elements that have an attribute value that matches 
	 * 	any value in the form string list. This method converts all attribute values to lowercase and assumes
	 *  that all values in the list of form input values are lowercase
	 * 
	 * @param formFields
	 * @param form_string_arr
	 * @return
	 */
	private ElementState getFormField(List<ElementState> formFields, List<String> input_types) {
		for(ElementState element: formFields) {
			for(String attr_value : element.getAttributes().values()) {
				for(String input_type : input_types) {
					if(attr_value.toLowerCase().contains(input_type)) {
						return element;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Generate {@link List} of {@link SimpleStep steps}
	 * 
	 * @param last_page
	 * @param elements
	 * @return
	 */
	private List<Step> generateSteps(PageState last_page, List<ElementState> elements) {
		List<Step> steps = new ArrayList<>();
		for(ElementState element: elements) {
			SimpleStep step = new SimpleStep(last_page, 
								 element, 
								 Action.CLICK, 
								 "", 
								 null);
			steps.add(step);
		}
		return steps;
	}

	private void addElementsToExploredMap(PageState last_page, List<ElementState> filtered_elements) {
		if(explored_elements.containsKey(last_page.getUrl())) {
			explored_elements.get(last_page.getUrl()).addAll(filtered_elements);
		}
		else {
			explored_elements.put(last_page.getUrl(), filtered_elements);
		}
	}
	

	private void addElementsToExploredMap(PageState last_page, ElementState element) {
		if(explored_elements.containsKey(last_page.getUrl())) {
			explored_elements.get(last_page.getUrl()).add(element);
		}
		else {
			List<ElementState> elements = new ArrayList<>();
			elements.add(element);
			explored_elements.put(last_page.getUrl(), elements);
		}
	}
	
	/**
	 * Checks if element is a link, a button element or class contains "btn or button"
	 * @param element {@link ElementState}
	 * 
	 * @return true if element is a link or button element or if element class contains "btn or button"
	 */
	private boolean isInteractiveElement(ElementState element) {
		return element.getName().contentEquals("a")
				|| element.getName().contentEquals("button")
				|| (element.getAttributes().containsKey("class")
					&& (element.getAttribute("class").contains("btn")
						|| element.getAttribute("class").contains("button")))
				|| (element.getAttributes().containsKey("type") 
					&& (element.getAttribute("type").contains("button")
						|| element.getAttribute("type").contains("checkbox")
						|| element.getAttribute("type").contains("email")
						|| element.getAttribute("type").contains("file")
						|| element.getAttribute("type").contains("radio")
						|| element.getAttribute("type").contains("tel")))
				|| element.getAttributes().containsKey("onclick");
	}
	
	private boolean isElementExplored(String page_url, ElementState element) {
		return explored_elements.containsKey(page_url) 
				&& explored_elements.get(page_url).contains(element);
	}
	
	private boolean isFormElement(ElementState element) {
		return element.getXpath().contains("form");
	}
}
