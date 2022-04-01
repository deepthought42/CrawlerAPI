package com.looksee.actors;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.browsing.Browser;
import com.looksee.browsing.form.ElementRuleExtractor;
import com.looksee.helpers.BrowserConnectionHelper;
import com.looksee.integrations.DeepthoughtApi;
import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.Form;
import com.looksee.models.PageState;
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.message.FormDiscoveredMessage;
import com.looksee.models.message.JourneyMessage;
import com.looksee.models.rules.Rule;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.FormService;
import com.looksee.utils.PathUtils;

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
	private BrowserService browser_service;
	
	@Autowired
	private FormService form_service;
	
	@Autowired
	private DomainService domain_service;
	
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
				.match(JourneyMessage.class, message -> {		
					log.warning("-------------------------------------------------------------------------------------------------");
					log.info("Retrieving first url in path objects");
					//get first url
					String url = PathUtils.getFirstUrl(message.getPathObjects());
					//get first page in path
					log.info("extracting host from url :: "+url);
				  	String host = new URL(url).getHost();
				  	Browser browser = null;
				  	boolean forms_created = false;
				  	int count = 0;
				  	Domain domain = domain_service.findById(message.getDomainId()).get();
				  	do{
				  		try{
				  			log.warning("form discovery getting browser connection ::   "+message.getBrowser().toString());
					  		browser = BrowserConnectionHelper.getConnection(message.getBrowser(), BrowserEnvironment.DISCOVERY);
					  		log.warning("FORM  Navigating to url    ::        "+url);
					  		browser.navigateTo(url);
							browser.removeDriftChat();

					  		log.warning("total path objects    ::   "+message.getPathObjects().size());
					  		//crawler.crawlPathWithoutBuildingResult(message.getKeys(), message.getPathObjects(), browser, host, message.getAccountId());

					  		PageState page_state = null;
							for(int idx=message.getPathObjects().size()-1; idx >= 0; idx--){
								if(message.getPathObjects().get(idx) instanceof PageState){
									page_state = (PageState)message.getPathObjects().get(idx);
									break;
								}
							}
							 
							log.warning("extracting all forms");
						  	Set<Form> forms = browser_service.extractAllForms(message.getAccountId(), domain, browser);
						  	log.warning("forms extracted :: "+forms.size());
						  	for(Form form : forms){
						  		//check if form exists before creating a new one
						  		
							  	for(Element field : form.getFormFields()){
									//for each field in the complex field generate a set of tests for all known rules
							  		List<Rule> rules = rule_extractor.extractInputRules(field);
									field.getRules().addAll(rules);
								}
							    DeepthoughtApi.predict(form);
							  
							    form = form_service.save(form);
							    FormDiscoveredMessage form_message = new FormDiscoveredMessage(form, page_state, message.getAccountId(), message.getDomainId());
							  	message.getDiscoveryActor().tell(form_message, getSelf());
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
					}while(!forms_created && count < 100);
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
