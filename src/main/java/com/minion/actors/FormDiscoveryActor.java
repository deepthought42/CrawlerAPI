package com.minion.actors;

import java.net.URL;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.browsing.form.ElementRuleExtractor;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.integrations.DeepthoughtApi;
import com.qanairy.models.ElementState;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.message.FormMessage;
import com.qanairy.models.message.PathMessage;
import com.qanairy.models.rules.Rule;
import com.qanairy.services.BrowserService;
import com.qanairy.utils.PathUtils;

import akka.actor.Props;
import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;


@Component
@Scope("prototype")
public class FormDiscoveryActor extends AbstractActor{
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private Crawler crawler;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private ElementRuleExtractor rule_extractor;
	
	public static Props props() {
	  return Props.create(FormDiscoveryActor.class);
	}
	
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

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(PathMessage.class, message -> {			
					//get first url
					String url = PathUtils.getFirstUrl(message.getPathObjects());
					//get first page in path
				  	String host = new URL(url).getHost();
				  	Browser browser = null;
				  	boolean forms_created = false;
				  	int count = 0;
				  	
				  	do{
				  		try{
				  			log.warning("form discovery getting browser connection ::   "+message.getBrowser().toString());
					  		browser = BrowserConnectionHelper.getConnection(message.getBrowser(), BrowserEnvironment.DISCOVERY);
					  		log.warning("FORM  Navigating to url    ::        "+url);
					  		browser.navigateTo(url);
					  		log.warning("total path objects    ::   "+message.getPathObjects().size());
					  		crawler.crawlPathWithoutBuildingResult(message.getKeys(), message.getPathObjects(), browser, host);

					  		PageState page_state = null;
							for(int idx=message.getPathObjects().size()-1; idx >= 0; idx--){
								if(message.getPathObjects().get(idx) instanceof PageState){
									page_state = (PageState)message.getPathObjects().get(idx);
									break;
								}
							}
							 
							log.warning("extracting all forms");
						  	List<Form> forms = browser_service.extractAllForms(page_state, browser);
						  	log.warning("forms extracted :: "+forms.size());
						  	for(Form form : forms){
						  		//check if form exists before creating a new one
						  		
							  	for(ElementState field : form.getFormFields()){
									//for each field in the complex field generate a set of tests for all known rules
							  		List<Rule> rules = rule_extractor.extractInputRules(field);
									field.getRules().addAll(rules);
								}
							    DeepthoughtApi.predict(form);
							  								  	
							    FormMessage form_message = new FormMessage(form, page_state, message.getAccountId());
							  	message.getDiscoveryActor().tell(form_message, getSelf());
							  	MessageBroadcaster.broadcastDiscoveredForm(form, host, message.getAccountId());
						  	}
						  	forms_created = true;
						  	return;
						} catch(Exception e){
					  		log.warning("Form discovery exception :: " + e.getMessage());
					  		forms_created = false;
					  		e.printStackTrace();
					  	}
				  		finally{
				  			if(browser != null){
				  				browser.close();
				  			}
				  		}
				  		count++;
					}while(!forms_created && count < 1000);
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
}
