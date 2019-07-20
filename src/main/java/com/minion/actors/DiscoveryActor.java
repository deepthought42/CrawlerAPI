package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.minion.api.MessageBroadcaster;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.enums.DiscoveryStatus;
import com.qanairy.models.enums.DomainAction;
import com.qanairy.models.enums.PathStatus;
import com.qanairy.models.message.DiscoveryActionMessage;
import com.qanairy.models.message.DomainActionMessage;
import com.qanairy.models.message.PathMessage;
import com.qanairy.models.message.UrlMessage;
import com.qanairy.services.AccountService;
import com.qanairy.services.DiscoveryRecordService;
import com.qanairy.services.DomainService;
import com.qanairy.services.EmailService;
import com.qanairy.services.TestService;
import com.qanairy.utils.PathUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

public class DiscoveryActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(ExploratoryBrowserActor.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());

	private static DiscoveryRecord discovery_record;
	
	private static ActorRef domain_actor;
	
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
						DiscoveryRecord discovery = new DiscoveryRecord(new Date(), message.getDomain().getDiscoveryBrowserName(), message.getDomain().getUrl(), 0, 1, 0, DiscoveryStatus.RUNNING);

						message.getAccount().addDiscoveryRecord(discovery_record);
						account_service.save(message.getAccount());

						message.getDomain().addDiscoveryRecord(discovery_record);
						domain_service.save(message.getDomain());

						//create new discovery
						discovery.getExpandedUrls().add(message.getDomain().getUrl());
						discovery_service.save(discovery);
						
						//start a discovery
						log.info("Sending URL to UrlBrowserActor");
						
						final ActorRef url_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("urlBrowserActor"), "urlBrowserActor"+UUID.randomUUID());
						//final ActorRef url_browser_actor = this.getContext().actorOf(Props.create(UrlBrowserActor.class), "UrlBrowserActor"+UUID.randomUUID());
						url_browser_actor.tell(acct_message, getSelf() );
					}
					else if(message.getAction().equals(DiscoveryAction.STOP)){
						//stop all discovery processes
					}
				})
				.match(UrlMessage.class, message -> {
					discovery_service.incrementTotalPathCount(discovery_record.getKey());
					//broadcast discovery
					MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

					if(!discovery_record.getExpandedUrls().contains(message.getUrl().toString())){
						discovery_record.addExpandedPageState(message.getUrl().toString());
						
						//send message to urlBrowserActor
						final ActorRef url_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("urlBrowserActor"), "urlBrowserActor"+UUID.randomUUID());
						//final ActorRef url_browser_actor = this.getContext().actorOf(Props.create(UrlBrowserActor.class), "UrlBrowserActor"+UUID.randomUUID());
						url_browser_actor.tell(message, getSelf() );
						
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
					}
				})
				.match(PathMessage.class, message -> {
					if(message.getStatus().equals(PathStatus.READY)){
						final ActorRef form_discoverer = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("formDiscoveryActor"), "form_discovery"+UUID.randomUUID());
						ActorRef path_expansion_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());

						PathMessage path_message = message.clone();

						form_discoverer.tell(path_message, getSelf() );
						path_expansion_actor.tell(path_message, getSelf() );
					}
					else if(message.getStatus().equals(PathStatus.EXPANDED)){
						//get last page state
						PageState page_state = PathUtils.getLastPageState(message.getPathObjects());
						
						discovery_service.incrementTotalPathCount(discovery_record.getKey());
						if(discovery_record.getExpandedPageStates().contains(page_state.getKey())){
							return;
						}
						else{
							discovery_record.setLastPathRanAt(new Date());
							discovery_record.addExpandedPageState(page_state.getKey());
							discovery_record = discovery_service.save(discovery_record);
						}
						
						log.info("existing total path count :: "+discovery_record.getTotalPathCount());
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
						
						final ActorRef exploratory_browser_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("exploratoryBrowserActor"), "exploratory_browser_actor"+UUID.randomUUID());
						exploratory_browser_actor.tell(message, getSelf() );
					}
					else if(message.getStatus().equals(PathStatus.EXPANDED)){
						DiscoveryRecord discovery_record = discovery_service.increaseExaminedPathCount(discovery_record.getKey(), 1);
						
						if(discovery_record.getExaminedPathCount() >= discovery_record.getTotalPathCount()){
					    	email_service.sendSimpleMessage(acct_msg.getAccountKey(), "The test has finished running", "Discovery on "+discovery_record.getDomainUrl()+" has finished. Visit the <a href='app.qanairy.com/discovery>Discovery panel</a> to start classifying your tests");
						}
						try{
							MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
					  	}catch(Exception e){
					  		log.error("Error sending discovery status from Exploratory Actor :: "+e.getMessage());
						}
					}
				})
				.match(Test.class, test -> {
			  		DiscoveryRecord discovery_record = discovery_service.incrementTestCount(discovery_record.getKey());
					//broadcast discovery
					MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

					//send message to Domain Actor
					domain_actor.tell(test, getSelf());
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
