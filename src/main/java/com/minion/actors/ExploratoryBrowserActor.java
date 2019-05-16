package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Group;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.message.TestCandidateMessage;
import com.qanairy.services.DiscoveryRecordService;
import com.qanairy.services.EmailService;
import com.qanairy.services.PageStateService;
import com.qanairy.services.TestService;

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
 */
@Component
@Scope("prototype")
public class ExploratoryBrowserActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(ExploratoryBrowserActor.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private EmailService email_service;
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DiscoveryRecordService discovery_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private Crawler crawler;
	
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
				.match(Message.class, message-> {
					Message<?> acct_msg = (Message<?>)message;
					if (acct_msg.getData() instanceof ExploratoryPath){
						String browser_name = acct_msg.getOptions().get("browser").toString();
						ExploratoryPath exploratory_path = (ExploratoryPath)acct_msg.getData();
						boolean candidate_identified = false;
						
						if(exploratory_path.getPathObjects() != null){
							PageState result_page = null;
		
							//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
							//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 						
							//for(Action action : exploratory_path.getPossibleActions()){
								//ExploratoryPath path = ExploratoryPath.clone(exploratory_path);
								//Action action_record = action_service.findByKey(action.getKey());
								
								//if(!exploratory_path.getPathKeys().contains("action")){
									
								/*
									if(action_record != null){
										action = action_record;
									}
									
									path.addPathObject(action);
									path.addToPathKeys(action.getKey());
									*/
								//}
								String page_url = acct_msg.getOptions().get("host").toString();

								result_page = crawler.performPathExploratoryCrawl(browser_name, exploratory_path, page_url);
								result_page = page_state_service.save(result_page);
								
								//have page checked for landability
								//Domain domain = domain_repo.findByHost(acct_msg.getOptions().get("host").toString());
							
								//get page states 
								List<PageState> page_states = new ArrayList<PageState>();
								for(PathObject path_obj : exploratory_path.getPathObjects()){
									if(path_obj instanceof PageState){
										PageState page_state = (PageState)path_obj;
										page_state.setElements(page_state_service.getElementStates(page_state.getKey()));
										page_states.add(page_state);										
									}
								}
								
								boolean isResultAnimatedState = isResultAnimatedState( page_states, result_page);
								
								if(!ExploratoryPath.hasCycle(page_states, result_page, exploratory_path.getPathObjects().size() == 1)
										&& !isResultAnimatedState){
									//check if result is an animated image from previous page
									
									log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
									log.warn("sending test candidate to parent path explorer");
									candidate_identified = true;
							  		//crawl test and get result
							  		//if this result is the same as the result achieved by the original test then replace the original test with this new test
									DiscoveryRecord discovery_record = discovery_service.increaseExaminedPathCount(acct_msg.getOptions().get("discovery_key").toString(), 1);

									TestCandidateMessage msg = new TestCandidateMessage(exploratory_path.getPathKeys(), exploratory_path.getPathObjects(), discovery_record, acct_msg.getAccountKey(), result_page, acct_msg.getOptions());
									ActorRef parent_path_explorer = actor_system.actorOf(SpringExtProvider.get(actor_system)
											.props("parentPathExplorer"), "parent_path_explorer"+UUID.randomUUID());
									parent_path_explorer.tell(msg, getSelf());
								}
							//}
							
							if(!candidate_identified){
								DiscoveryRecord discovery_record = discovery_service.increaseExaminedPathCount(acct_msg.getOptions().get("discovery_key").toString(), 1);
								//send email if this is the last test
						  		if(discovery_record.getExaminedPathCount() >= discovery_record.getTotalPathCount()){
							    	email_service.sendSimpleMessage(acct_msg.getAccountKey(), "Discovery on "+discovery_record.getDomainUrl()+" has finished. Visit the <a href='app.qanairy.com/discovery>Discovery panel</a> to start classifying your tests", "The test has finished running");
								}
								try{
									MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
							  	}catch(Exception e){
							  		log.error("Error sending discovery status from Exploratory Actor :: "+e.getMessage());
								}
							}
						}
		
						
						//PLACE CALL TO LEARNING SYSTEM HERE
						//Brain.learn(test, test.getIsUseful());
					}

					//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);
		
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
	
	private boolean isResultAnimatedState(List<PageState> page_states, PageState result_page) {
		for(PageState page_state : page_states){
			if(!page_state.getAnimatedImageUrls().isEmpty()){
				for(String image_url : page_state.getAnimatedImageUrls()){
					//if result screenshot image url is the same as a previous animated state then return true
					if(result_page.getScreenshotUrl().equals(image_url)){
						return true;
					}
				}
			}
		}
		
		return false;
	}

	public static List<String> getPageTransition(Browser browser) throws MalformedURLException{
		List<String> transition_keys = new ArrayList<String>();
		boolean transition_detected = false;
		
		long start_ms = System.currentTimeMillis();
		//while (time passed is less than 30 seconds AND transition has occurred) or transition_detected && loop not detected
		String last_key = null;
		do{
			URL url = new URL(browser.getDriver().getCurrentUrl());
			String url_string = url.getProtocol()+"://"+url.getHost()+"/"+url.getPath();
			log.warn("current url retrieved");
			
			int element_count = browser.getDriver().findElements(By.xpath("//*")).size();
			String new_key = url_string+":"+element_count;
			log.warn("page key generated :: " + new_key);
			
			transition_detected = (last_key != null && !new_key.equals(last_key));
			log.warn("Is a transition occurring  :   " + transition_detected);
			
			log.warn("transition keys size :: " + transition_keys.size());
			last_key = new_key;
			
			if(transition_detected){
				log.warn("setting transition start time to now");
				start_ms = System.currentTimeMillis();
				transition_keys.add(new_key);
			}
			//transition is detected if keys are different
		}while((System.currentTimeMillis() - start_ms) < 5000);
		
		return transition_keys;
	}
	
	/**
	 * Adds Group labeled "form" to test if the test has any elements in it that have form in the xpath
	 * 
	 * @param test {@linkplain Test} that you want to label
	 */
	private void addFormGroupsToPath(Test test) {
		//check if test has any form elements
		for(PathObject path_obj: test.getPathObjects()){
			if(path_obj.getClass().equals(ElementState.class)){
				ElementState elem = (ElementState)path_obj;
				if(elem.getXpath().contains("form")){
					test.addGroup(new Group("form"));
					test_service.save(test, test.firstPage().getUrl()); 
					break;
				}
			}
		}
	}
}
