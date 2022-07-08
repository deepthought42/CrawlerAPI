package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
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
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.SubscriptionService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.JourneyUtils;
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
	private AccountService account_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	private Account account;
	private ActorRef audit_manager;
	
	Map<String, Boolean> visited_urls = new HashMap<>();
	
	private Map<String, List<ElementState>> explored_elements = new HashMap<>();
	
	//PROGRESS TRACKING VARIABLES
	private int examined_journeys = 0;
	private int generated_journeys = 0;
	private int page_count = 0;
	
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
					audit_manager = getContext().getParent();
					if(CrawlAction.START.equals(message.getAction())) {
						log.warn("STARTING crawl actor. Sending message to page state builder");
						
						
						ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
								  .props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
						page_state_builder.tell(message, getSelf());
					}
					else if(CrawlAction.STOP.equals(message.getAction())) {
						
					}
				})
				.match(PageDataExtractionMessage.class, msg -> {
					log.warn("page data extraction message received..."+msg.getPageState().getUrl());
					if(this.account == null) {
						this.account = account_service.findById(msg.getAccountId()).get();
					}
					SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());

					if(subscription_service.hasExceededDomainPageAuditLimit(plan, page_count)) {
						log.warn("Account has exceeded domain page audit limit for subscription..."+page_count);
						
						//send message to audit manager that subscription was exceeded
						//NOTE: Make sure audit manager broadcasts message to user indicating they have exceeded subscription
						
						AuditRecord record = audit_record_service.findById(msg.getAuditRecordId()).get();
						record.setStatus(ExecutionStatus.EXCEEDED_SUBSCRIPTION);
						audit_record_service.save(record, msg.getAccountId(), msg.getDomainId());
						
						Account account = account_service.findById(msg.getAccountId()).get();
						
						MessageBroadcaster.broadcastSubscriptionExceeded(account);
						
					}
					else {
					
						page_count++;
						
						log.warn("path expansion actor received PageDataExtraction Message");
						List<ElementState> elements = msg.getPageState().getElements();
	
						//Filter out non interactive elements
						//Filter out elements that are in explored map for PageState with key
						//Filter out form elements
						List<ElementState> filtered_elements = elements.parallelStream()
																		.filter(element -> element.isVisible())
																		.filter(element -> {
																				Dimension dimension = new Dimension(element.getWidth(), element.getHeight()); 
																				Point location = new Point(element.getXLocation(), element.getYLocation());
																				return BrowserService.hasWidthAndHeight(dimension) && !BrowserService.doesElementHaveNegativePosition(location);
																		})
																		.filter(element -> !BrowserService.isStructureTag(element.getName()))
																		.filter(element -> isInteractiveElement(element))
																		.filter(element -> !isElementExplored(msg.getPageState().getUrl(), element))
																		.filter(element -> !isFormElement(element))
																		.collect(Collectors.toList());
						
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
						for(Step step: form_steps) {
							List<Step> step_list = new ArrayList<>();
							step_list.add(step);
							JourneyMessage journey_msg = new JourneyMessage(new ArrayList<>(step_list), 
																			PathStatus.EXPANDED, 
																			BrowserType.CHROME, 
																			msg.getDomainId(), 
																			msg.getAccountId(),
																			msg.getAuditRecordId());
							generated_journeys++;
							ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
									.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
							journey_executor.tell(journey_msg, getSelf());
						}
						
						//send form elements to form journey actor
						//Add interactive elements to explored map for PageState key
						addElementsToExploredMap(msg.getPageState(), filtered_elements);
						//Build Steps for all elements in list
						List<Step> new_steps = generateSteps(msg.getPageState(), filtered_elements);
						
						//generate new journeys with new steps and send to journey executor to be evaluated
						for(Step step: new_steps) {
							generated_journeys++;
							List<Step> steps = new ArrayList<>();
							steps.add(step);
							JourneyMessage journey_msg = new JourneyMessage(new ArrayList<>(steps), 
																			PathStatus.EXPANDED, 
																			BrowserType.CHROME, 
																			msg.getDomainId(), 
																			msg.getAccountId(),
																			msg.getAuditRecordId());
							
							ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
																	.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
							journey_executor.tell(journey_msg, getSelf());
						}
						
						//send generated and examined journey counts to audit manager
						log.warn("# examined journeys :: "+examined_journeys);
						log.warn("# generated journeys :: "+generated_journeys);
	
						JourneyExaminationProgressMessage progress_msg = new JourneyExaminationProgressMessage(msg.getAccountId(), 
																											   msg.getAuditRecordId(), 
																											   msg.getDomainId(),
																											   examined_journeys,
																											   generated_journeys);
						audit_manager.tell(progress_msg, getSelf());
					}
				})
				.match(BrowserCrawlActionMessage.class, message -> {
					ActorRef page_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
														.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
					page_builder.tell(message, getSelf());
				})
				.match(DiscardedJourneyMessage.class, message -> {
					examined_journeys++;
					//send generated and examined journey counts to audit manager
					log.warn("# examined journeys :: "+examined_journeys);
					log.warn("# generated journeys :: "+generated_journeys);
					JourneyExaminationProgressMessage progress_msg = new JourneyExaminationProgressMessage(message.getAccountId(), 
																										   message.getAuditRecordId(), 
																										   message.getDomainId(),
																										   examined_journeys,
																										   generated_journeys);
					audit_manager.tell(progress_msg, getSelf());
				})
				.match(ConfirmedJourneyMessage.class, message -> {
					audit_manager.tell(message.clone(), getSelf());

					SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());

					log.warn("crawler received confirmed journey message :: "+message.getSteps().size() + " steps");
					examined_journeys++;
					JourneyExaminationProgressMessage progress_msg = new JourneyExaminationProgressMessage(message.getAccountId(), 
							   message.getAuditRecordId(), 
							   message.getDomainId(),
							   examined_journeys,
							   generated_journeys);
					audit_manager.tell(progress_msg, getSelf());
					
					PageState final_page = PathUtils.getLastPageState(message.getSteps());
					Domain domain = domain_service.findById(message.getDomainId()).get();
					if( BrowserUtils.isExternalLink(domain.getUrl(), final_page.getUrl()) 
							|| visited_urls.containsKey(final_page.getUrl())) {//|| visited_pages.contains(final_page)/*|| pageState is in visited list*/) {
						return;
					}

					//Add page state to visited list
					visited_urls.put(final_page.getUrl(), Boolean.TRUE);
					if(subscription_service.hasExceededDomainPageAuditLimit(plan, page_count)) {
						log.warn("account has exceeded subscription plan");
						return;
					}
							
					//retrieve all ElementStates from journey
					List<ElementState> elements = final_page.getElements();
					//Filter out non interactive elements
					//Filter out elements that are in explored map for PageState with key
					//Filter out form elements
					List<ElementState> filtered_elements = elements.parallelStream()
																	.filter(element -> element.isVisible())
																	.filter(element -> {
																			Dimension dimension = new Dimension(element.getWidth(), element.getHeight()); 
																			Point location = new Point(element.getXLocation(), element.getYLocation());
																			return BrowserService.hasWidthAndHeight(dimension) && !BrowserService.doesElementHaveNegativePosition(location);
																	})
																	.filter(element -> element.getName().contentEquals("a") && element.getAttribute("href") != null && !(BrowserUtils.isExternalLink(domain.getUrl(), element.getAttribute("href")) || element.getAttribute("href").startsWith("#")))
																	.filter(element -> !BrowserService.isStructureTag(element.getName()))
																	.filter(element -> isInteractiveElement(element))
																	.filter(element -> !isElementExplored(final_page.getUrl(), element))
																	.filter(element -> !isFormElement(element))
																	.collect(Collectors.toList());
					
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
						
						if(steps_list.contains(step)) {
							log.warn("FORM STEP EXISTS IN STEP LIST ALREADY");
						}
						else {
							steps_list.add(step);
							generated_journeys++;
							JourneyMessage journey_msg = new JourneyMessage(new ArrayList<>(steps_list), 
																			PathStatus.EXPANDED, 
																			BrowserType.CHROME, 
																			message.getDomainId(), 
																			message.getAccountId(),
																			message.getAuditRecordId());
							
							ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
														.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
							journey_executor.tell(journey_msg, getSelf());
						}
					}
					
					//send form elements to form journey actor
					//Add interactive elements to explored map for PageState key
					addElementsToExploredMap(final_page, filtered_elements);
					//Build Steps for all elements in list
					List<Step> new_steps = generateSteps(final_page, filtered_elements);
					
					if(new_steps.isEmpty()) {
						return;
					}
					
					//generate new journeys with new steps and send to journey executor to be evaluated
					for(Step step: new_steps) {
						generated_journeys++;
						
						List<Step> cloned_steps = JourneyUtils.trimPreLoginSteps(message.getSteps());
						
						
						cloned_steps.add(step);
						JourneyMessage journey_msg = new JourneyMessage(new ArrayList<>(cloned_steps), 
																		PathStatus.EXPANDED, 
																		BrowserType.CHROME, 
																		message.getDomainId(), 
																		message.getAccountId(),
																		message.getAuditRecordId());
						ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
																.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
						journey_executor.tell(journey_msg, getSelf());
					}
					
					//send generated and examined journey counts to audit manager
					log.warn("# examined journeys :: "+examined_journeys);
					log.warn("# generated journeys :: "+generated_journeys);
					progress_msg = new JourneyExaminationProgressMessage(message.getAccountId(), 
																										   message.getAuditRecordId(), 
																										   message.getDomainId(),
																										   examined_journeys,
																										   generated_journeys);
					audit_manager.tell(progress_msg, getSelf());
				})
				/* commenting out as potentially unused. Remove if still present after 6/30/2022
				.match(PageDataExtractionMessage.class, message -> {
					SubscriptionPlan plan = SubscriptionPlan.create(account.getSubscriptionType());

					if(!subscription_service.hasExceededDomainPageAuditLimit(plan, page_count)) {
						return;
					}
					
					page_count++;
					
					log.warn("path expansion actor received PageDataExtraction Message");
					List<ElementState> elements = message.getPageState().getElements();

					//Filter out non interactive elements
					//Filter out elements that are in explored map for PageState with key
					//Filter out form elements
					List<ElementState> filtered_elements = elements.parallelStream()
																	.filter(element -> element.isVisible())
																	.filter(element -> {
																			Dimension dimension = new Dimension(element.getWidth(), element.getHeight()); 
																			return BrowserService.hasWidthAndHeight(dimension) && !BrowserService.doesElementHaveNegativePosition(location);
																	})
																	.filter(element -> element.getXLocation() >= 0 && element.getYLocation() >= 0)
																	.filter(element -> isInteractiveElement(element))
																	.filter(element -> !isElementExplored(message.getPageState().getKey(), element))
																	.filter(element -> !isFormElement(element))
																	.collect(Collectors.toList());
					//send form elements to form journey actor
					//Add interactive elements to explored map for PageState key
					addElementsToExploredMap(message.getPageState(), filtered_elements);
					//Build Steps for all elements in list
					List<Step> new_steps = generateSteps(message.getPageState(), filtered_elements);
					
					//generate new journeys with new steps and send to journey executor to be evaluated
					for(Step step: new_steps) {
						generated_journeys++;
						List<Step> steps = new ArrayList<>();
						steps.add(step);
						JourneyMessage journey_msg = new JourneyMessage(steps, 
																		PathStatus.EXPANDED, 
																		BrowserType.CHROME, 
																		message.getDomainId(), 
																		message.getAccountId(),
																		message.getAuditRecordId());
						
						ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
																.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
						journey_executor.tell(journey_msg, getSelf());
					}
					
					//send generated and examined journey counts to audit manager
					log.warn("# examined journeys :: "+examined_journeys);
					log.warn("# generated journeys :: "+generated_journeys);
					JourneyExaminationProgressMessage progress_msg = new JourneyExaminationProgressMessage(message.getAccountId(), 
																										   message.getAuditRecordId(), 
																										   message.getDomainId(),
																										   examined_journeys,
																										   generated_journeys);
					audit_manager.tell(progress_msg, getSelf());
				})*/
				.match(MemberUp.class, mUp -> {
					log.debug("Member is Up: {}", mUp.member());
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
	
	private List<Step> generateFormSteps(long domain_id, PageState pageState, Set<Form> forms) {
		List<Step> steps = new ArrayList<>();
		for(Form form: forms) {
			
			if(FormType.LOGIN.equals(form.getType())){
				//retrieve user credentials
				List<TestUser> user_list = domain_service.findTestUsers(domain_id);
				if(!user_list.isEmpty()) {
					
					
					//get username field
					ElementState username_element = getFormField(form.getFormFields(), "username");
					//create step to enter username into username field
					
					//get password field
					ElementState password_element = getFormField(form.getFormFields(), "password");
	
					//create step to enter password into password field
					//get submit button
					ElementState submit_btn = form.getSubmitField();
					
					log.warn("username element :: "+username_element.getKey());
					log.warn("password element :: "+password_element.getKey());
					log.warn("Submit button :: "+submit_btn.getKey());
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
