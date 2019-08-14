package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.net.URL;
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

import com.minion.api.MessageBroadcaster;
import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.enums.DiscoveryStatus;
import com.qanairy.models.enums.PathStatus;
import com.qanairy.models.message.DiscoveryActionMessage;
import com.qanairy.models.message.FormDiscoveryMessage;
import com.qanairy.models.message.PathMessage;
import com.qanairy.models.message.TestMessage;
import com.qanairy.models.message.UrlMessage;
import com.qanairy.services.AccountService;
import com.qanairy.services.BrowserService;
import com.qanairy.services.DiscoveryRecordService;
import com.qanairy.services.DomainService;
import com.qanairy.services.EmailService;
import com.qanairy.utils.PathUtils;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.TrackMessage;

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
public class DiscoveryActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(DiscoveryActor.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());

	private DiscoveryRecord discovery_record;
		
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private DiscoveryRecordService discovery_service;
	
	@Autowired
	private EmailService email_service;
	
	private ActorRef domain_actor;
	private ActorRef url_browser_actor;
	private ActorRef form_discoverer;
	private ActorRef form_test_discovery_actor;
	private ActorRef path_expansion_actor;
	private List<ActorRef> exploratory_browser_actors = new ArrayList<>();
	private BrowserType browser;
	
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
					}
					else if(message.getAction().equals(DiscoveryAction.STOP)){
						//look up discovery record if it's null
						stopDiscovery(message);
					}
				})
				.match(PathMessage.class, message -> {
					if(message.getStatus().equals(PathStatus.READY)){
						PathMessage path_message = message.clone();
						log.warn("discovery record in discovery actor :: " + discovery_record);
						
						discovery_record = getDiscoveryRecord(message.getDomain().getUrl(), message.getDomain().getDiscoveryBrowserName());
						discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
						
						String path_key = String.join(":::", message.getKeys());
						//if(!discovery_record.getExpandedPathKeys().contains(path_key)){	
							if(path_expansion_actor == null){
								path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
						    }

							path_expansion_actor.tell(path_message, getSelf() );
							
							if(form_discoverer == null){
								form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("formDiscoveryActor"), "form_discovery"+UUID.randomUUID());
							}
							form_discoverer.tell(path_message, getSelf() );
						//}
					}
					else if(message.getStatus().equals(PathStatus.EXPANDED)){
						//get last page state
						discovery_record = getDiscoveryRecord(message.getDomain().getUrl(), message.getDomain().getDiscoveryBrowserName());
						discovery_record.setLastPathRanAt(new Date());
						
						//check if key already exists before adding to prevent duplicates
						discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
													

						if(exploratory_browser_actors.isEmpty()){
							//create multiple exploration actors for parallel execution
							for(int i=0; i < 5; i++){
								exploratory_browser_actors.add(actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("exploratoryBrowserActor"), "exploratory_browser_actor"+UUID.randomUUID()));
							}
						}
						exploratory_browser_actors.get(discovery_record.getTotalPathCount()%(exploratory_browser_actors.size()-1)).tell(message, getSelf() );
					}
					else if(message.getStatus().equals(PathStatus.EXAMINED)){
						
						String path_key = String.join(":::", message.getKeys());
						if(!discovery_record.getExpandedPathKeys().contains(path_key)){				
							discovery_record.getExpandedPathKeys().add(path_key);
						}
						//increment examined count
						discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
						
						//check if discovery is considered complete, if so then update status and send email to all accounts
						if(discovery_record.getExaminedPathCount() >= discovery_record.getTotalPathCount() && discovery_record.getTotalPathCount()> 2){
							List<Account> accounts = discovery_service.getAccounts(discovery_record.getKey());
							discovery_record.setStatus(DiscoveryStatus.COMPLETE);
							discovery_service.save(discovery_record);
							
							for(Account account: accounts){
								email_service.sendSimpleMessage(account.getUsername(), "The discovery has finished running for "+discovery_record.getDomainUrl(), "Discovery on "+discovery_record.getDomainUrl()+" has finished. Visit the <a href='app.qanairy.com/discovery>Discovery panel</a> to start classifying your tests");
							}
						}
					}
					MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

					discovery_service.save(discovery_record);

				})
				.match(TestMessage.class, test_msg -> {
					Test test = test_msg.getTest();
					discovery_record.setTestCount(discovery_record.getTestCount()+1);
					if(domain_actor == null){
						domain_actor = test_msg.getDomainActor();
					}
					//send message to Domain Actor
					domain_actor.tell(test_msg, getSelf());
					/*
					 * TODO: uncomment once ready for pricing again.
			    	Account acct = account_service.findByUsername(message.getAccountKey());
			    	if(subscription_service.hasExceededSubscriptionDiscoveredLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
			    		throw new PaymentDueException("Your plan has 0 discovered tests left. Please upgrade to run a discovery");
			    	}
			    	*/
					
					boolean isLandable = BrowserService.checkIfLandable(test_msg.getDomain().getDiscoveryBrowserName(), test.getResult(), test );
					
					String path_key = String.join(":::", test.getPathKeys());

					log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					log.warn("expanded path key :: " + path_key);
					log.warn("does discovery record contain expanded path key :: " + discovery_record.getExpandedPathKeys().contains(path_key));
					log.warn("does discovery record contain expanded page url already? :: " + discovery_record.getExpandedUrls().contains(path_key));
					log.warn("test spans multiple domains : :   " + test.getSpansMultipleDomains());
					log.warn("Test result is landable?    ::   "+isLandable);
					log.warn("Result url   ::::   " + test.getResult().getUrl());
					log.warn("expanded path key :: " + path_key);
					log.warn("does discovery record contain expanded path key :: " + discovery_record.getExpandedPathKeys().contains(path_key));
					log.warn("does discovery record contain expanded page url already? :: " + discovery_record.getExpandedUrls().contains(path_key));
					log.warn("test spans multiple domains : :   " + test.getSpansMultipleDomains());
					log.warn("Test result is landable?    ::   "+isLandable);
					log.warn("Result url   ::::   " + test.getResult().getUrl());
					log.warn("test first page url :: " + test.findLastPage().getUrl());
					log.warn("test length :: " + test.getPathObjects().size());
					if(!test.getSpansMultipleDomains()){
						log.warn("test doesn't span multiple domains");
						if(isLandable ){
							if(url_browser_actor == null){
								url_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
										  .props("urlBrowserActor"), "urlBrowserActor"+UUID.randomUUID());
							}
							log.warn("SENDING LANDABLE RESULT TO URL BROWSER ACTOR :: "+test.getResult().getUrl());
							log.warn("Sending browser form tst msg :: " + test_msg.getDomain());
							log.warn("Sending browser form tst msg :: " + test_msg.getDomain().getDiscoveryBrowserName());
							
							UrlMessage url_message = new UrlMessage(getSelf(), new URL(test.getResult().getUrl()), browser, domain_actor, test_msg.getDomain());
							url_browser_actor.tell(url_message, getSelf() );
						}
						else {
							List<String> final_key_list = new ArrayList<>(test.getPathKeys());
				  			final_key_list.add(test.getResult().getKey());
				  			List<PathObject> final_object_list = new ArrayList<>(test.getPathObjects());
				  			final_object_list.add(test.getResult());
				  			//run reducer on key list
				  			final_key_list = PathUtils.reducePathKeys(final_key_list);
				  			
				  			log.warn("sending test to path expansion actor");
				  			PathMessage path = new PathMessage(final_key_list, final_object_list, getSelf(), PathStatus.EXAMINED, browser, domain_actor, test_msg.getDomain());
				  			if(path_expansion_actor == null){
				  				path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				  						  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
				  		    }
					  		//send path message with examined status to discovery actor
							path_expansion_actor.tell(path, getSelf());
						}
					}
					log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					
					MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

					discovery_service.save(discovery_record);
				})
				.match(FormDiscoveryMessage.class, form_msg -> {
					log.warn("form msg :: " + form_msg);
					log.warn("form domain :: " + form_msg.getDomain());
					log.warn("url  :: " + form_msg.getDomain().getUrl());
					log.warn("form browser :: " + form_msg.getDomain().getDiscoveryBrowserName());
					discovery_record = getDiscoveryRecord(form_msg.getDomain().getUrl(), form_msg.getDomain().getDiscoveryBrowserName());
					//look up discovery for domain and increment
			        discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
			        form_msg.setDiscoveryActor(getSelf());
			        log.info("Sending form message  :: "+form_msg.toString());
		    		
			        if(form_test_discovery_actor == null){
			        	form_test_discovery_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
			  				  .props("formTestDiscoveryActor"), "form_test_discovery_actor"+UUID.randomUUID());
			        }
		        	form_test_discovery_actor.tell(form_msg, ActorRef.noSender());

			        discovery_service.save(discovery_record);
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

	private DiscoveryRecord getDiscoveryRecord(String url, String browser) {
		DiscoveryRecord discovery_record = null;
		if(this.discovery_record == null){
			log.warn("discovery actor is null for instance variable in discovery actor");
			discovery_record = domain_service.getMostRecentDiscoveryRecord(url);
			
			if(discovery_record == null){
				log.warn("was unable to find running discovery record in db");
				discovery_record = new DiscoveryRecord(new Date(), browser, url,
						0, 0, 0,
						DiscoveryStatus.RUNNING);
			}
			return discovery_record;
		}
		
		return this.discovery_record;
	}

	private void startDiscovery(DiscoveryActionMessage message) throws MalformedURLException {
		browser = message.getBrowser();
		domain_actor = getSender();
		//create actors for discovery
		if(url_browser_actor == null){
			url_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("urlBrowserActor"), "urlBrowserActor"+UUID.randomUUID());
		}
		
		if(form_discoverer == null){
			form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("formDiscoveryActor"), "form_discovery"+UUID.randomUUID());
		}
		
		if(path_expansion_actor == null){
			path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
	    }
		
		if(form_test_discovery_actor == null){
        	form_test_discovery_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
  				  .props("formTestDiscoveryActor"), "form_test_discovery_actor"+UUID.randomUUID());
        }
    
		if(exploratory_browser_actors.isEmpty()){
			//create multiple exploration actors for parallel execution
			for(int i=0; i < 5; i++){
				exploratory_browser_actors.add(actor_system.actorOf(SpringExtProvider.get(actor_system)
						  .props("exploratoryBrowserActor"), "exploratory_browser_actor"+UUID.randomUUID()));
			}
		}
		
		discovery_record = new DiscoveryRecord(new Date(), message.getDomain().getDiscoveryBrowserName(), message.getDomain().getUrl(), 0, 1, 0, DiscoveryStatus.RUNNING);
		//create new discovery
		discovery_service.save(discovery_record);

		message.getAccount().addDiscoveryRecord(discovery_record);
		account_service.save(message.getAccount());

		message.getDomain().addDiscoveryRecord(discovery_record);
		domain_service.save(message.getDomain());
		
		//start a discovery
		log.info("Sending URL to UrlBrowserActor");
		UrlMessage url_message = new UrlMessage(getSelf(), new URL(message.getDomain().getProtocol() + "://"+message.getDomain().getUrl()), message.getBrowser(), domain_actor, message.getDomain());
		url_browser_actor.tell(url_message, getSelf() );
		
		//Fire discovery started event
    	Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();

		Map<String, String> traits = new HashMap<String, String>();
        traits.put("user_id", message.getAccount().getUserId());
        traits.put("url", message.getDomain().getUrl());
    	traits.put("browser", message.getBrowser().toString());
        traits.put("discovery_started", "true");
    	traits.put("discovery_key", discovery_record.getKey());
        analytics.enqueue(TrackMessage.builder("Started Discovery")
    		    .userId(message.getAccount().getUsername())
    		    .properties(traits)
		);
	}

	private void stopDiscovery(DiscoveryActionMessage message) {
		if(discovery_record == null){
			discovery_record = domain_service.getMostRecentDiscoveryRecord(message.getDomain().getUrl());
		}
		
		discovery_record.setStatus(DiscoveryStatus.STOPPED);
		discovery_service.save(discovery_record);
		
		//stop all discovery processes
		if(url_browser_actor != null){
			actor_system.stop(url_browser_actor);
			url_browser_actor = null;
		}
		if(path_expansion_actor != null){
			actor_system.stop(path_expansion_actor);
			path_expansion_actor = null;
		}
		if(form_discoverer != null){
			actor_system.stop(form_discoverer);
			form_discoverer = null;
		}
		if(!exploratory_browser_actors.isEmpty()){	
			for(ActorRef actor : exploratory_browser_actors){
				actor_system.stop(actor);
			}
			exploratory_browser_actors = new ArrayList<>();
		}
	}
}
