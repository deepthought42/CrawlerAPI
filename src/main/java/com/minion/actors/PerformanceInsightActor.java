package com.minion.actors;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.pagespeedonline.Pagespeedonline;
import com.google.api.services.pagespeedonline.model.LighthouseAuditResultV5;
import com.google.api.services.pagespeedonline.model.LighthouseCategoryV5.AuditRefs;
import com.google.api.services.pagespeedonline.model.PagespeedApiPagespeedResponseV5;
import com.minion.browsing.Browser;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.BugType;
import com.qanairy.models.enums.CaptchaResult;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.enums.FormFactor;
import com.qanairy.models.enums.InsightType;
import com.qanairy.models.experience.AccessibilityDetailNode;
import com.qanairy.models.experience.AssetSize;
import com.qanairy.models.experience.AssetSizeOpportunityDetail;
import com.qanairy.models.experience.Audit;
import com.qanairy.models.experience.AuditDetail;
import com.qanairy.models.experience.BlockingResource;
import com.qanairy.models.experience.BootUpTime;
import com.qanairy.models.experience.CachingDetail;
import com.qanairy.models.experience.DiagnosticDetail;
import com.qanairy.models.experience.DomSize;
import com.qanairy.models.experience.FinalScreenshot;
import com.qanairy.models.experience.GroupWorkBreakdown;
import com.qanairy.models.experience.NetworkRequestDetail;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.models.experience.ResourceSummary;
import com.qanairy.models.experience.ScreenshotThumbnailDetails;
import com.qanairy.models.experience.ThirdPartySummaryDetail;
import com.qanairy.models.experience.WebPImageDetail;
import com.qanairy.models.message.BugMessage;
import com.qanairy.models.message.DiscoveryActionRequest;
import com.qanairy.models.message.UrlMessage;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.ElementState;
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.services.AuditService;
import com.qanairy.services.BrowserService;
import com.qanairy.services.BugMessageService;
import com.qanairy.services.DomainService;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.PageService;
import com.qanairy.services.PageStateService;
import com.qanairy.services.PerformanceInsightService;
import com.qanairy.utils.BrowserUtils;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse
 *
 */
@Component
@Scope("prototype")
public class PerformanceInsightActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(PerformanceInsightActor.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());
	private static String api_key = "AIzaSyD8jtPtAdC8g6gIEIidZnsDFEANE-2gSRY";

	@Autowired
	private ActorSystem actor_system;

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageService page_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;

	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private PerformanceInsightService performance_insight_service;
	
	@Autowired
	private BugMessageService bug_message_service;
	
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
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(UrlMessage.class, message -> {
					Timeout timeout = Timeout.create(Duration.ofSeconds(120));
					Future<Object> future = Patterns.ask(message.getDomainActor(), new DiscoveryActionRequest(message.getDomain(), message.getAccount()), timeout);
					DiscoveryAction discovery_action = (DiscoveryAction) Await.result(future, timeout.duration());
					
					if(discovery_action == DiscoveryAction.STOP) {
						log.warn("path message discovery actor returning");
						return;
					}
					
					String url = message.getUrl().toString();
					String host = message.getUrl().getHost();
					String browser_name = message.getDomain().getDiscoveryBrowserName();
					BrowserType browser_type = BrowserType.create(browser_name);
					Page page = null;
					
					do{
						Browser browser = null;
						
						try{
							browser = BrowserConnectionHelper.getConnection(browser_type, BrowserEnvironment.DISCOVERY);
							log.warn("navigating to url :: "+url);
							browser.navigateTo(url);
							BrowserUtils.getPageTransition(url, browser, host);
						  	BrowserUtils.getLoadingAnimation(browser, host);

							page = browser_service.buildPage(message.getAccount(), browser.getDriver().getCurrentUrl());
							log.warn("page returned :: "+page);
							PageState page_state = browser_service.buildPageState(message.getAccount(), message.getDomain(), browser);
							page_state_service.save(message.getAccount(), message.getDomain().getUrl(), page_state);
							log.warn("page state returned:: " +page_state);
							page = page_service.save(message.getAccount(), page);
							domain_service.addPage(message.getDomain().getUrl(), page, message.getAccount());
							page_service.addPageState(message.getAccount(), page.getKey(), page_state);
							
							//log.warn("page states count :: " + page.getPageStates().size());
							PagespeedApiPagespeedResponseV5 page_speed_response = getPageInsights(page.getUrl());
							log.warn("page speed response length :: " + page_speed_response.toPrettyString().length());
							
							PerformanceInsight performance_insight = extractInsights(message.getAccount(), message.getDomain().getUrl(), page_speed_response);
							
							//Page page = new Page(page.getUrl());
							page.setPerformanceScore(performance_insight.getSpeedScore());
							page.setAccessibilityScore(performance_insight.getAccessibilityScore());
							page.setSeoScore(performance_insight.getSeoScore());
							page = page_service.save(message.getAccount(), page);
							//domain_service.addPageState(message.getDomain().getUrl(), page_state, message.getAccount());
							performance_insight_service.save(performance_insight);
							page_service.addPerformanceInsight(message.getAccount(), message.getDomain().getUrl(), page.getKey(), performance_insight.getKey());
							domain_service.addPage(message.getDomain().getUrl(), page, message.getAccount());

							break;
						}
						catch(Exception e){
							log.warn("URL BROWSER ACTOR EXCEPTION :: "+e.getMessage());
							e.printStackTrace();
						}
						finally {
							if(browser != null){
								browser.close();
							}
						}
					}while(page == null);			
					log.warn("creating landing page performance and SEO insights");					
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
	
	/**
	 * Extract page speed insights data and performance audits
	 * 
	 * @param page_speed_response
	 * @return
	 */
	private PerformanceInsight extractInsights(String user_id, String domain_url, PagespeedApiPagespeedResponseV5 page_speed_response) {
		log.warn("captcha result :: "+page_speed_response.getCaptchaResult());
		log.warn("form factor :: "+page_speed_response.getLighthouseResult().getConfigSettings().getEmulatedFormFactor() );
		log.warn("date :: "+page_speed_response.getAnalysisUTCTimestamp());
		PerformanceInsight speed_insight = new PerformanceInsight(
				new Date(),
				page_speed_response.getLighthouseResult().getTiming().getTotal(),
				page_speed_response.getId(),
				page_speed_response.getLighthouseResult().getConfigSettings().getLocale(),
				CaptchaResult.create(page_speed_response.getCaptchaResult()),
				page_speed_response.getLighthouseResult().getRunWarnings(),
				FormFactor.create(page_speed_response.getLighthouseResult().getConfigSettings().getEmulatedFormFactor() ));
	    
	    if(page_speed_response.getLighthouseResult().getRuntimeError() != null) {
	    	speed_insight.setRuntimeErrorCode( page_speed_response.getLighthouseResult().getRuntimeError().getCode() );
	    	speed_insight.setRuntimeErrorMessage( page_speed_response.getLighthouseResult().getRuntimeError().getMessage() );
	    }
	    
	    log.warn("speed insight object built...");
	    
	    Map<String, LighthouseAuditResultV5> audit_map = page_speed_response.getLighthouseResult().getAudits();
	    
	    log.warn("accessiblity exists :: "+page_speed_response.getLighthouseResult().getCategories().getAccessibility().toString());
	    Map<InsightType, List<String>> audit_ref_map = new HashMap<>();
	    List<AuditRefs> audit_refs = page_speed_response.getLighthouseResult().getCategories().getPerformance().getAuditRefs();
	    for(AuditRefs ref : audit_refs) {
	    	if(!audit_ref_map.containsKey(InsightType.PERFORMANCE)) {
	    		audit_ref_map.put(InsightType.PERFORMANCE, new ArrayList<String>());
	    	}
	    	audit_ref_map.get(InsightType.PERFORMANCE).add(ref.getId());
	    }
	    
	    audit_refs = page_speed_response.getLighthouseResult().getCategories().getAccessibility().getAuditRefs();
	    for(AuditRefs ref : audit_refs) {
	    	if(!audit_ref_map.containsKey(InsightType.ACCESSIBILITY)) {
	    		audit_ref_map.put(InsightType.ACCESSIBILITY, new ArrayList<String>());
	    	}
	    	audit_ref_map.get(InsightType.ACCESSIBILITY).add(ref.getId());
	    }
	    
    	for(LighthouseAuditResultV5 audit_record  : audit_map.values()) {
    		InsightType insight_type = getAuditType(audit_record, audit_ref_map);
    		
    		if(InsightType.ACCESSIBILITY.equals(insight_type)) {
    			Audit accessibility_audit = new Audit( audit_record.getId(),
								 					   audit_record.getDescription(),
								 					   audit_record.getDisplayValue(),
								 					   audit_record.getErrorMessage(),
								 					   audit_record.getExplanation(),
								 					   audit_record.getNumericValue(),
								 					   audit_record.getScoreDisplayMode(),
								 					   audit_record.getTitle(),
								 					   extractAccessibilityAuditDetails(user_id, audit_record.getDetails()));
    			Double score = convertScore(audit_record.getScore());
    			accessibility_audit.setScore(score);
 			   	accessibility_audit.setType(insight_type);
 			   	accessibility_audit = audit_service.save(accessibility_audit);
 			   
 			   	speed_insight.addAudit(accessibility_audit);
    		}
    		else {
    			Audit audit = new Audit(
    					   audit_record.getId(),
    					   audit_record.getDescription(),
    					   audit_record.getDisplayValue(),
    					   audit_record.getErrorMessage(),
    					   audit_record.getExplanation(),
    					   audit_record.getNumericValue(),
    					   audit_record.getScoreDisplayMode(),
    					   audit_record.getTitle(),
    					   extractAuditDetails(audit_record.getId(), audit_record.getDetails()));
    			   Double score = convertScore(audit_record.getScore());
    			   audit.setScore(score);
    			   audit.setType(insight_type);
    			   audit = audit_service.save(audit);
    			   
    			   speed_insight.addAudit(audit);
    		}		   
    	}
    	
    	double speed_score = convertScore(page_speed_response.getLighthouseResult().getCategories().getPerformance().getScore());
    	double accessibility_score = convertScore(page_speed_response.getLighthouseResult().getCategories().getAccessibility().getScore());
    	double seo_score = convertScore(page_speed_response.getLighthouseResult().getCategories().getSeo().getScore());
    	speed_insight.setSeoScore(seo_score);
    	speed_insight.setAccessibilityScore(accessibility_score);
    	speed_insight.setSpeedScore(speed_score);
    	log.warn("speed insight audits found :: "+speed_insight.getAudits().size());
    	return performance_insight_service.save(speed_insight);
	}

	/**
	 * 
	 * @param details
	 * 
	 * @return
	 * 
	 * @pre details != null;
	 */
	private List<AuditDetail> extractAccessibilityAuditDetails(String user_id, Map<String, Object> details) {
		List<AuditDetail> audit_details = new ArrayList<>();
		if(details != null) {
			List<Object> items = (List)details.get("items");
			
			for(Object item: items) {
				ArrayMap<String, Object> obj = (ArrayMap)item;
				log.warn("Object :: " + obj);
				log.warn("Object node :: " + obj.get("node"));
				ArrayMap<String, Object> json_obj = (ArrayMap)obj.get("node");
				String explanation = json_obj.get("explanation").toString();
				String snippet = json_obj.get("snippet").toString();
				snippet = snippet.replaceAll(">\\s+<","> <");
				snippet = snippet.replace("\n", "");
				snippet = snippet.replace("required=\"\"", "required");
				log.warn("user id :: "+user_id);
				log.warn("snippet ::  " + snippet);
				ElementState element_state = element_state_service.findByOuterHtml(user_id, snippet);
				log.warn("element state found with outer html :: "+element_state);

				explanation = explanation.replace("Fix all of the following:", "");
				explanation = explanation.replace("Fix any of the following:", "");

				String[] explanations = explanation.split("\\n");
				for(String err : explanations) {
					if(err.trim().isEmpty()) {
						continue;
					}
					BugMessage error = new BugMessage(err, BugType.ACCESSIBILITY, new Date());
					log.warn("Error :: "+err);
					audit_details.add(error);
					error = bug_message_service.save(error);
					log.warn("saved bug message : " + error.getMessage());
					log.warn("element state :: " + element_state);
					element_state_service.addBugMessage( element_state.getId(), error );
				}
			}
		}
		
		return audit_details;
	}
	
	/**
	 * 
	 * @param details
	 * @return
	 * 
	 * @pre details != null;
	 */
	private List<AuditDetail> extractAuditDetails(String name, Map<String, Object> details) {
		
		List<AuditDetail> audit_details = new ArrayList<>();
		if(details != null) {
			List<Object> items = (List<Object>)details.get("items");
			if(items == null) {
				return audit_details;
			}
			for(Object item: items) {
				ArrayMap<String, Object> detail_obj = (ArrayMap)item;

				if("screenshot-thumbnails".contentEquals(name) || "final-screenshot".contentEquals(name)){
					AuditDetail audit_detail = new ScreenshotThumbnailDetails((Integer)detail_obj.get("timing"), (Long)detail_obj.get("timestamp"), detail_obj.get("data").toString());
					audit_details.add(audit_detail);
				}
				else if("resource-summary".contentEquals(name)) {
					AuditDetail audit_detail = new ResourceSummary(Integer.parseInt(detail_obj.get("requestCount").toString()), detail_obj.get("resourceType").toString(), detail_obj.get("label").toString(), Long.parseLong(detail_obj.get("size").toString()));
					audit_details.add(audit_detail);
				}
				else if("render-blocking-resources".contentEquals(name)) {
					AuditDetail audit_detail = new BlockingResource(detail_obj.get("url").toString(), Integer.parseInt(detail_obj.get("totalBytes").toString()), Double.parseDouble(detail_obj.get("wastedMs").toString()));
					audit_details.add(audit_detail);
				}
				else if("font-display".contentEquals(name)) {
					AuditDetail audit_detail = new BlockingResource(detail_obj.get("url").toString(), null, Double.parseDouble(detail_obj.get("wastedMs").toString()));
					audit_details.add(audit_detail);
				}
				else if("user-timings".contentEquals(name)) {
					
				}
				else if("main-thread-tasks".contentEquals(name)) {
					
				}
				else if("bootup-time".contentEquals(name)) {
					AuditDetail audit_detail = new BootUpTime(detail_obj.get("url").toString(), 
															  Double.parseDouble(detail_obj.get("total").toString()),
															  Double.parseDouble(detail_obj.get("scripting").toString()),
															  Double.parseDouble(detail_obj.get("scriptParseCompile").toString()));
					audit_details.add(audit_detail);
				}
				else if("total-byte-weight".contentEquals(name)) {
					AuditDetail audit_detail = new AssetSize(detail_obj.get("url").toString(), Integer.parseInt(detail_obj.get("totalBytes").toString()));
					audit_details.add(audit_detail);
				}
				else if("mainthread-work-breakdown".contentEquals(name)) {
					AuditDetail audit_detail = new GroupWorkBreakdown(detail_obj.get("group").toString(), Double.parseDouble(detail_obj.get("duration").toString()), detail_obj.get("groupLabel").toString());
					audit_details.add(audit_detail);
				}
				else if("color-contract".contentEquals(name) 
						|| "frame-title".contentEquals(name) 
						|| "link-name".contentEquals(name) 
						|| "meta-viewport".contentEquals(name) 
						|| "label".contentEquals(name)
				) {
					JSONObject node = (JSONObject)detail_obj.get("node");
					AuditDetail audit_detail = new AccessibilityDetailNode(node.get("nodeLabel").toString(), node.get("explanation").toString(), node.get("type").toString(), node.get("selector").toString(), node.get("path").toString(), node.get("snippet").toString());
					audit_details.add(audit_detail);
				}
				else if("dom-size".contentEquals(name)) {
					AuditDetail audit_detail = new DomSize(detail_obj.get("statistic").toString(), detail_obj.get("value").toString(), (Map)detail_obj.get("element"));
					audit_details.add(audit_detail);
				}
				else if("metrics".contentEquals(name)) {
					//AuditDetail audit_detail = new MetricsDetail();
					//audit_details.add(audit_detail);
				}
				else if("uses-long-cache-ttl".contentEquals(name)) {
					//if debug data is null this might create issues 
					AuditDetail audit_detail = new CachingDetail(detail_obj.get("url").toString(), Double.parseDouble(detail_obj.get("wastedBytes").toString()), Integer.parseInt(detail_obj.get("totalBytes").toString()), Double.parseDouble(detail_obj.get("cacheHitProbability").toString()), Integer.parseInt(detail_obj.get("cacheLifetimeMs").toString()),  (Map)detail_obj.get("debugData"));
					audit_details.add(audit_detail);
				}
				else if("unminified-css".contentEquals(name) || "unused-css-rules".contentEquals(name) || "offscreen-images".contentEquals(name) || "uses-responsive-images".contentEquals(name)) {
					AuditDetail audit_detail = new AssetSizeOpportunityDetail(detail_obj.get("url").toString(), Integer.parseInt(detail_obj.get("wastedBytes").toString()), Double.parseDouble(detail_obj.get("wastedPercent").toString()), Integer.parseInt(detail_obj.get("totalBytes").toString()));
					audit_details.add(audit_detail);
				}
				else if("third-party-summary".contentEquals(name)) {
					log.warn("details request transfer size :: " + detail_obj.get("transferSize"));
					log.warn("details request blocking time :: " + detail_obj.get("blockingTime"));
					log.warn("details request main thread time :: " + detail_obj.get("mainThreadTime"));
					log.warn("details request entity :: " + detail_obj.get("entity"));
	
					AuditDetail audit_detail = new ThirdPartySummaryDetail(Integer.parseInt(detail_obj.get("transferSize").toString()), Double.parseDouble(detail_obj.get("blockingTime").toString()), Double.parseDouble(detail_obj.get("mainThreadTime").toString()), (Map)detail_obj.get("entity"));
					audit_details.add(audit_detail);
				}
				else if("network-requests".contentEquals(name)) {
					log.warn("details object value :: "+detail_obj);

					String resource_type = null;
					if(detail_obj.get("resourceType") != null) {
						resource_type = detail_obj.get("resourceType").toString();
					}
					AuditDetail audit_detail = new NetworkRequestDetail(Integer.parseInt(detail_obj.get("transferSize").toString()), 
																		detail_obj.get("url").toString(), 
																		Integer.parseInt(detail_obj.get("statusCode").toString()), 
																		resource_type,
																		detail_obj.get("mimeType").toString(),
																		Integer.parseInt(detail_obj.get("resourceSize").toString()),
																		Double.parseDouble(detail_obj.get("endTime").toString()),
																		Double.parseDouble(detail_obj.get("startTime").toString()));
					audit_details.add(audit_detail);
				}
				else if("uses-webp-images".contentEquals(name)) {
					AuditDetail audit_detail = new WebPImageDetail(Integer.parseInt(detail_obj.get("wastedBytes").toString()), detail_obj.get("url").toString(), Boolean.getBoolean(detail_obj.get("fromProtocol").toString()), Boolean.getBoolean(detail_obj.get("isCrossOrigin").toString()), Integer.parseInt(detail_obj.get("totalBytes").toString()));
					audit_details.add(audit_detail);
				}
				else if("diagnostics".contentEquals(name)) {
					AuditDetail audit_detail = new DiagnosticDetail(Integer.parseInt(detail_obj.get("numStylesheets").toString()), 
																	Double.parseDouble(detail_obj.get("throughput").toString()),
																	Integer.parseInt(detail_obj.get("numTasksOver10ms").toString()), 
																	Integer.parseInt(detail_obj.get("numTasksOver25ms").toString()), 
																	Integer.parseInt(detail_obj.get("numTasksOver50ms").toString()), 
																	Integer.parseInt(detail_obj.get("numTasksOver100ms").toString()),
																	Integer.parseInt(detail_obj.get("numTasksOver500ms").toString()), 
																	Integer.parseInt(detail_obj.get("numRequests").toString()),
																	Double.parseDouble(detail_obj.get("totalTaskTime").toString()),
																	Integer.parseInt(detail_obj.get("mainDocumentTransferSize").toString()),
																	Integer.parseInt(detail_obj.get("totalByteWeight").toString()),
																	Integer.parseInt(detail_obj.get("numTasks").toString()),
																	Double.parseDouble(detail_obj.get("rtt").toString()),
																	Double.parseDouble(detail_obj.get("maxRtt").toString()),
																	Integer.parseInt(detail_obj.get("numFonts").toString()),
																	Integer.parseInt(detail_obj.get("numScripts").toString()));
					audit_details.add(audit_detail);
				}
				else if("".contentEquals(name)) {
					
				}
				else if("".contentEquals(name)) {
					
				}
			}
		}
	
		return audit_details;
	}

	private InsightType getAuditType(
			LighthouseAuditResultV5 audit_record,
			Map<InsightType, List<String>> audit_ref_map
	) {
		for(InsightType type: audit_ref_map.keySet()) {
			for(String audit_id : audit_ref_map.get(type)) {
				if(audit_record.getId().equals(audit_id)){
					return type;
				}
			}
		}
		
		return InsightType.UNKNOWN;
	}

	private Double convertScore(Object score_obj) {
		Double score = null;
		try {
			score = ((BigDecimal)score_obj).doubleValue();
		}
		catch(Exception e) {
			//e.printStackTrace();
			score = new Double(0);
		}
		
		return score;
	}
	
	/**
	 * Retrieves Google PageSpeed Insights result from their API
	 * 
	 * @param url
	 * 
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * 
	 * @pre url != null
	 * @pre !url.isEmpty()
	 */
	private PagespeedApiPagespeedResponseV5 getPageInsights(String url) throws IOException, GeneralSecurityException {
	    assert url != null;
	    assert !url.isEmpty();
	    
		JacksonFactory jsonFactory = new JacksonFactory();
	    NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

	    HttpRequestInitializer httpRequestInitializer = null; //this can be null here!
	    Pagespeedonline p = new Pagespeedonline.Builder(transport, jsonFactory, httpRequestInitializer).build();
	    
	    Pagespeedonline.Pagespeedapi.Runpagespeed runpagespeed  = p.pagespeedapi().runpagespeed(url).setKey(api_key);
	    List<String> category = new ArrayList<>();
	    category.add("performance");
	    category.add("accessibility");
	    //category.add("best-practices");
	    //category.add("pwa");
	    category.add("seo");
	    runpagespeed.setCategory(category);
	    return runpagespeed.execute();
	}
}
