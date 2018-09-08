package com.minion.actors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.structs.Message;
import com.qanairy.models.Form;
import com.qanairy.models.FormRecord;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.rules.Rule;
import com.qanairy.services.BrowserService;

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
	Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	ElementRuleExtractor extractor;
	
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
				.match(Message.class, message -> {
					if(message.getData() instanceof PageState){
	
						PageState page_state = ((PageState)message.getData());
						
						//get first page in path
			
						int cnt = 0;
					  	Browser browser = null;
					  	
					  	while(browser == null && cnt < 5){
					  		try{
						  		browser = new Browser(message.getOptions().get("browser").toString());
						  		browser.navigateTo(page_state.getUrl());
								break;
							}catch(NullPointerException e){
								log.error(e.getMessage());
							}
							cnt++;
						}	
					  	
					  	List<Form> forms = browser_service.extractAllForms(page_state, browser);
					  	for(Form form : forms){
					  		String rl_response = "";
						  	for(PageElement field: form.getFormFields()){
								//for each field in the complex field generate a set of tests for all known rules
						  		List<Rule> rules = extractor.extractInputRules(field);
								
								log.info("Total RULES   :::   "+rules.size());
								for(Rule rule : rules){
									field.addRule(rule);
								
								}
							}
						  	
						  	ObjectMapper mapper = new ObjectMapper();

					        //Object to JSON in String
					        String form_json = mapper.writeValueAsString(form);
					        
					        
					        /*
					        CloseableHttpClient client = HttpClients.createDefault();
					        HttpPost httpPost = new HttpPost("http://rl.qanairy.com");
					     
					        //String json = "{"+id+":1,"+name+":"+John+"}";
					        StringEntity entity = new StringEntity(form_json);
					        httpPost.setEntity(entity);
					        httpPost.setHeader("Accept", "application/json");
					        httpPost.setHeader("Content-type", "application/json");
					     
					        CloseableHttpResponse response = client.execute(httpPost);
					        client.close();
					        */
					        
					        System.err.println("FORM JSON :: "+form_json);
						  	
						  	System.err.println("Requesting prediction for form from RL system");
						  	//STAGING URL
						  	HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 

						  	 HttpPost request = new HttpPost("http://198.211.117.122/predict");
						     StringEntity params =new StringEntity(form_json);
						     request.addHeader("content-type", "application/json; charset=UTF-8");
						     request.setEntity(params);
						     HttpResponse response = httpClient.execute(request);

						  	System.err.println("Recieved status code from RL :: "+response.getStatusLine().getStatusCode());
						  	int status = response.getStatusLine().getStatusCode();
						  	
					        switch (status) {
					            case 200:
					            case 201:
					            	
					                BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
					                StringBuilder sb = new StringBuilder();
					                String line;
					                while ((line = br.readLine()) != null) {
					                    sb.append(line+"\n");
					                }
					                br.close();
					                rl_response = sb.toString();
					                System.err.println("Response received from RL system :: "+rl_response);
					        }
					        String src = "";
					        
					        System.err.println("form tag :: "+form.getFormTag());
					        System.err.println("form tax xpath :: "+form.getFormTag().getXpath());
					        
					        WebElement element = browser.getDriver().findElement(By.xpath(form.getFormTag().getXpath()));
					        src = element.getAttribute("innerHTML");

						  	
						  	try{
						  		browser.close();
						  	}
						  	catch(Exception e){}
						  	
					        FormType[] form_types = new FormType[1];
							form_types[0] = FormType.LOGIN;
							double[] weights = new double[1];
							weights[0] = 0.3;
					        FormRecord form_record = new FormRecord(src, form, "screenshot_url", page_state, weights, form_types, FormStatus.DISCOVERED);
					        
						  	MessageBroadcaster.broadcastDiscoveredForm(form_record, message.getOptions().get("host").toString());
					  	}
					}
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
					System.err.println("o class :: "+o.getClass().getName());
					log.info("received unknown message");
				})
				.build();
	}
}
