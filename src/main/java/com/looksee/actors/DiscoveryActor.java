package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.analytics.SegmentAnalyticsHelper;
import com.looksee.api.MessageBroadcaster;
import com.looksee.api.exception.PaymentDueException;
import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;
import com.looksee.models.Form;
import com.looksee.models.LookseeObject;
import com.looksee.models.PageState;
import com.looksee.models.Test;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.DiscoveryAction;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.PathStatus;
import com.looksee.models.message.AccountRequest;
import com.looksee.models.message.DiscoveryActionMessage;
import com.looksee.models.message.FormDiscoveredMessage;
import com.looksee.models.message.FormDiscoveryMessage;
import com.looksee.models.message.PathMessage;
import com.looksee.models.message.TestMessage;
import com.looksee.models.message.UrlMessage;
import com.looksee.services.AccountService;
import com.looksee.services.DiscoveryRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.EmailService;
import com.looksee.services.FormService;
import com.looksee.services.PageStateService;
import com.looksee.services.SubscriptionService;
import com.looksee.services.TestService;
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

/**
 * 
 * 
 */
@Component
@Scope("prototype")
public class DiscoveryActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(DiscoveryActor.class.getName());

	private final int DISCOVERY_ACTOR_COUNT = 200;

	private Cluster cluster = Cluster.get(getContext().getSystem());
	private DiscoveryRecord discovery_record;
		
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageStateService page_state_service;
		
	@Autowired
	private TestService test_service;
	
	@Autowired
	private DiscoveryRecordService discovery_service;
	
	@Autowired
	private EmailService email_service;
	
	@Autowired
	private FormService form_service;
	
	@Autowired
	private SubscriptionService subscription_service;
	
	private Map<String, PageState> explored_pages = new HashMap<>();
	private Account account;
	private ActorRef domain_actor;
	private ActorRef url_browser_actor;
	private ActorRef form_discoverer;
	private ActorRef path_expansion_actor;

	private List<ActorRef> exploratory_browser_actors = new ArrayList<>();
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
				.match(DiscoveryActionMessage.class, message-> {
					if(message.getAction().equals(DiscoveryAction.START)){
						startDiscovery(message);
						setAccount(account_service.findByUserId(message.getAccountId()));
					}
					else if(message.getAction().equals(DiscoveryAction.STOP)){
						//look up discovery record if it's null
						stopDiscovery(message);
					}
				})
				.match(PathMessage.class, message -> {
					/*
					Timeout timeout = Timeout.create(Duration.ofSeconds(30));
					Future<Object> future = Patterns.ask(message.getDomainActor(), new DiscoveryActionRequest(message.getDomain(), message.getAccountId()), timeout);
					DiscoveryAction discovery_action = (DiscoveryAction) Await.result(future, timeout.duration());
					
					if(discovery_action == DiscoveryAction.STOP) {
						log.warn("path message discovery actor returning");
						return;
					}
					*/
					discovery_record = getDiscoveryRecord(message.getDomain().getUrl(), BrowserType.CHROME.toString(), message.getAccountId());

					if(message.getStatus().equals(PathStatus.READY)){
						PathMessage path_message = message.clone();
						log.warn("discovery record in discovery actor :: " + discovery_record);
						
						discovery_record = getDiscoveryRecord(message.getDomain().getUrl(), BrowserType.CHROME.toString(), message.getAccountId());
						discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
						
						if(path_expansion_actor == null){
							path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
					    }

						path_expansion_actor.tell(path_message, getSelf() );
					}
					else if(message.getStatus().equals(PathStatus.EXPANDED)){
						//get last page state
						discovery_record = getDiscoveryRecord(message.getDomain().getUrl(), BrowserType.CHROME.toString(), message.getAccountId());
						discovery_record.setLastPathRanAt(new Date());
						
						//check if key already exists before adding to prevent duplicates
						discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
													
						if(exploratory_browser_actors.isEmpty()){
							//create multiple exploration actors for parallel execution
							for(int i=0; i < DISCOVERY_ACTOR_COUNT; i++){
								exploratory_browser_actors.add(actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("exploratoryBrowserActor"), "exploratory_browser_actor"+UUID.randomUUID()));
							}
						}
						exploratory_browser_actors.get(discovery_record.getTotalPathCount()%(exploratory_browser_actors.size()-1)).tell(message, getSelf() );
					}
					else if(message.getStatus().equals(PathStatus.EXAMINED)){
						
						String path_key = String.join(":", message.getKeys());
						if(!discovery_record.getExpandedPathKeys().contains(path_key)){				
							discovery_record.getExpandedPathKeys().add(path_key);
						}
						//increment examined count
						discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
						
						//check if discovery is considered complete, if so then update status and send email to all accounts
						if(discovery_record.getExaminedPathCount() >= discovery_record.getTotalPathCount() && discovery_record.getTotalPathCount()> 2){
							List<Account> accounts = discovery_service.getAccounts(discovery_record.getKey());
							discovery_record.setStatus(ExecutionStatus.COMPLETE);
							
							for(Account account: accounts){
								email_service.sendSimpleMessage(account.getEmail(), "The discovery has finished running for "+discovery_record.getDomainUrl(), "Discovery on "+discovery_record.getDomainUrl()+" has finished. Visit the <a href='app.looksee.com/discovery>Discovery panel</a> to start classifying your tests");
							}
						}
					}
					MessageBroadcaster.broadcastDiscoveryStatus(discovery_record, message.getAccountId());

					discovery_service.save(discovery_record);
				})
				.match(TestMessage.class, test_msg -> {
					
					//plan exceeded check
			    	Account acct = account_service.findByUserId(test_msg.getAccount());
			    	if(subscription_service.hasExceededSubscriptionDiscoveredLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
			    		throw new PaymentDueException("Your plan has 0 generated tests left. Please upgrade to generate more tests");
			    	}
					discovery_record = getDiscoveryRecord(test_msg.getDomain().getUrl(), BrowserType.CHROME.toString(), test_msg.getAccount());
					Test test = test_msg.getTest();
					Test existing_record = test_service.findByKey(test.getKey(), test_msg.getDomain().getUrl(), test_msg.getAccount());
					if(existing_record == null) {
						discovery_record.setTestCount(discovery_record.getTestCount()+1);
						try {
							SegmentAnalyticsHelper.testCreated(test_msg.getAccount(), test.getKey());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
					if(domain_actor == null){
						domain_actor = test_msg.getDomainActor();
					}
					//send message to Domain Actor
					domain_actor.tell(test_msg, getSelf());
					
					//boolean isLandable = BrowserService.checkIfLandable(test.getResult(), test )  || !BrowserService.testContainsElement(test.getPathKeys());
					BrowserType browser = BrowserType.create(discovery_record.getBrowserName());
					log.warn("test spans multiple domains??    ::  "+test.getSpansMultipleDomains());
					
					if(!test.getSpansMultipleDomains()){
						/*
						Timeout timeout = Timeout.create(Duration.ofSeconds(120));
						Future<Object> future = Patterns.ask(domain_actor, new DiscoveryActionRequest(test_msg.getDomain(), test_msg.getAccount()), timeout);
						DiscoveryAction discovery_action = (DiscoveryAction) Await.result(future, timeout.duration());
						
						if(discovery_action == DiscoveryAction.STOP) {
							return;
						}
						*/
						
						List<String> final_key_list = new ArrayList<>(test.getPathKeys());
						final_key_list.add(test.getResult().getKey());
						List<LookseeObject> final_object_list = new ArrayList<>(test.getPathObjects());
						final_object_list.add(test.getResult());
						log.warn("test.getResult() element states  :: "+test.getResult().getElements().size());
						//run reducer on key list
						final_key_list = PathUtils.reducePathKeys(final_key_list);
						final_object_list = PathUtils.reducePathObjects(final_object_list);
						
						PathMessage path = new PathMessage(final_key_list, final_object_list, getSelf(), PathStatus.EXAMINED, browser, domain_actor, test_msg.getDomain(), test_msg.getAccount());
						
						if( !test.getResult().isLoginRequired() && test.getPathKeys().size() > 1){
							log.warn("explored pages contains element...."+(!explored_pages.containsKey(test.getResult().getUrl())));
							if(!explored_pages.containsKey(test.getResult().getUrl())) {
								explored_pages.put(test.getResult().getUrl(), test.getResult());
								if(url_browser_actor == null){
									url_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
											  .props("urlBrowserActor"), "urlBrowserActor"+UUID.randomUUID());
								}
								UrlMessage url_message = new UrlMessage(getSelf(), new URL(test.getResult().getUrl()), browser, domain_actor, test_msg.getDomain(), test_msg.getAccount());
								url_browser_actor.tell( url_message, getSelf() );
						    }
						}
						else {
							if(path_expansion_actor == null){
				  				path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				  						  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
				  		    }
					  		//send path message with examined status to discovery actor
							path_expansion_actor.tell( path, getSelf() );
							

							if(form_discoverer == null){
								form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("formDiscoveryActor"), "form_discovery_actor"+UUID.randomUUID());
							}
							form_discoverer.tell(path, getSelf());
						}
						
					}
					MessageBroadcaster.broadcastDiscoveryStatus(discovery_record, test_msg.getAccount());

					discovery_service.save(discovery_record);
				})
				.match(FormDiscoveryMessage.class, form_msg -> {
					discovery_record = getDiscoveryRecord(form_msg.getDomain().getUrl(), BrowserType.CHROME.toString(), form_msg.getAccountId());
					//look up discovery for domain and increment
			        discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
			        form_msg.setDiscoveryActor(getSelf());

			        discovery_service.save(discovery_record);
				})
				.match(AccountRequest.class, account_request_msg -> {
					getSender().tell(this.getAccount(), getSelf());
				})
				.match(FormDiscoveredMessage.class, form_msg -> {
					Form form = form_msg.getForm();
					try {
						SegmentAnalyticsHelper.formDiscovered(form_msg.getUserId(), form.getKey());
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					try {
					    form = form_service.save(form_msg.getUserId(), form_msg.getDomain().getUrl(), form);
					}catch(Exception e) {
						try {
							SegmentAnalyticsHelper.sendFormSaveError(form_msg.getUserId(), e.getMessage());
						} catch (Exception se) {
							se.printStackTrace();
						}
					}

					log.warn("form message page key :: "+form_msg.getPage().getKey());
					PageState page_state_record = page_state_service.findByKeyAndUsername(form_msg.getUserId(), form_msg.getPage().getKey());
					log.warn("form message page key :: "+page_state_record.getKey());

					
					try {
						page_state_service.saveUserAndDomain(form_msg.getUserId(), form_msg.getDomain().getUrl(), page_state_record);					    
					}catch(Exception e) {
						try {
							SegmentAnalyticsHelper.sendPageStateError(form_msg.getUserId(), e.getMessage());
						} catch (Exception se) {
							se.printStackTrace();
						}
					}
					
				  	MessageBroadcaster.broadcastDiscoveredForm(form, form_msg.getDomain().getUrl(), form_msg.getUserId());					
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

	private DiscoveryRecord getDiscoveryRecord(String url, String browser, String user_id) {

		
		return this.discovery_record;
	}

	private void startDiscovery(DiscoveryActionMessage message) throws IOException, GeneralSecurityException {
		domain_actor = getSender();
		
		//create actors for discovery
		if(url_browser_actor == null){
			url_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("urlBrowserActor"), "urlBrowserActor"+UUID.randomUUID());
		}
		
		if(form_discoverer == null){
			form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("formDiscoveryActor"), "form_discovery_actor"+UUID.randomUUID());
		}
		
		if(path_expansion_actor == null){
			path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
	    }
    
		if(exploratory_browser_actors.isEmpty()){
			//create multiple exploration actors for parallel execution
			for(int i=0; i < DISCOVERY_ACTOR_COUNT; i++){
				exploratory_browser_actors.add(actor_system.actorOf(SpringExtProvider.get(actor_system)
						  .props("exploratoryBrowserActor"), "exploratory_browser_actor"+UUID.randomUUID()));
			}
		}
		
		//create new discovery
		discovery_service.save(discovery_record);

		Account account = account_service.findByUserId(message.getAccountId());
		account = account_service.save(account);

		domain_service.save(message.getDomain());
		
		//start a discovery
		log.info("Sending URL to UrlBrowserActor");
		URL url = new URL(message.getDomain().getUrl());
		UrlMessage url_message = new UrlMessage(getSelf(), url, message.getBrowser(), domain_actor, message.getDomain(), message.getAccountId());
		
		url_browser_actor.tell(url_message, getSelf() );
	}

	

	private void stopDiscovery(DiscoveryActionMessage message) {
		
		log.warn("stopping discovery...");
		discovery_record.setStatus(ExecutionStatus.STOPPED);
		discovery_service.save(discovery_record);
	}
	
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}
	
	
}
