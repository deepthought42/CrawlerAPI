package com.minion.actors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.browsing.form.FormField;
import com.minion.structs.Message;
import com.qanairy.models.Form;
import com.qanairy.models.FormRecord;
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
						  	for(FormField field: form.getFormFields()){
								//for each field in the complex field generate a set of tests for all known rules
								List<Rule> rules = ElementRuleExtractor.extractInputRules(field.getInputElement());
								
								log.info("Total RULES   :::   "+rules.size());
								for(Rule rule : rules){
									field.getInputElement().addRule(rule);
								
								}
							}
						  	
						  	try{
						  		browser.close();
						  	}
						  	catch(Exception e){}
						  	
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
					        
					        
						  	byte[] out = form_json.getBytes(StandardCharsets.UTF_8);
						  	int length = out.length;

						  	System.err.println("Requesting prediction for form from RL system");
						  	//STAGING URL
						  	URL url = new URL("http://198.211.117.122/predict");
						  	URLConnection con = url.openConnection();
						  	HttpURLConnection http = (HttpURLConnection)con;
						  	http.setFixedLengthStreamingMode(length);
						  	http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
						  	http.connect();
						  	try(OutputStream os = http.getOutputStream()) {
						  	    os.write(out);
						  	}

						  	int status = http.getResponseCode();
						  	System.err.println("Recieved status code from RL :: "+status);
						  	
					        switch (status) {
					            case 200:
					            case 201:
					                BufferedReader br = new BufferedReader(new InputStreamReader(http.getInputStream()));
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
					        WebElement element = browser.getDriver().findElement(By.xpath(form.getFormTag().getXpath()));
					        src = element.getAttribute("innerHTML");

					        FormType[] form_types = new FormType[1];
							form_types[0] = FormType.LOGIN;
							double[] weights = new double[1];
							weights[0] = 0.3;
					        FormRecord form_record = new FormRecord(src, form, "screenshot_url", page_state, weights, form_types, FormStatus.DISCOVERED);
					        form_record.setForm(form);
					        
						  	MessageBroadcaster.broadcastDiscoveredForm(form_record, (new URL(page_state.getUrl()).getHost()));
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
