package com.minion.actors;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.pagespeedonline.Pagespeedonline;
import com.google.api.services.pagespeedonline.model.LighthouseAuditResultV5;
import com.google.api.services.pagespeedonline.model.LighthouseCategoryV5.AuditRefs;
import com.google.api.services.pagespeedonline.model.PagespeedApiPagespeedResponseV5;

import com.qanairy.models.enums.CaptchaResult;
import com.qanairy.models.enums.FormFactor;
import com.qanairy.models.enums.InsightType;
import com.qanairy.models.experience.AccessibilityDetailNode;
import com.qanairy.models.experience.AssetSize;
import com.qanairy.models.experience.AssetSizeOpportunityDetail;
import com.qanairy.models.experience.AuditDetail;
import com.qanairy.models.experience.BlockingResource;
import com.qanairy.models.experience.BootUpTime;
import com.qanairy.models.experience.CachingDetail;
import com.qanairy.models.experience.DiagnosticDetail;
import com.qanairy.models.experience.DomSize;
import com.qanairy.models.experience.GroupWorkBreakdown;
import com.qanairy.models.experience.MetricsDetail;
import com.qanairy.models.experience.NetworkRequestDetail;
import com.qanairy.models.experience.PageSpeedAudit;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.models.experience.ResourceSummary;
import com.qanairy.models.experience.ScreenshotThumbnailDetails;
import com.qanairy.models.experience.ThirdPartySummaryDetail;
import com.qanairy.models.experience.TimingDetail;
import com.qanairy.models.experience.WebPImageDetail;
import com.qanairy.models.message.UrlMessage;
import com.qanairy.models.ElementState;
import com.qanairy.models.Page;
import com.qanairy.services.AuditDetailService;
import com.qanairy.services.PageSpeedAuditService;
import com.qanairy.services.BrowserService;
import com.qanairy.services.DomainService;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.PageService;
import com.qanairy.services.PageStateService;
import com.qanairy.services.PerformanceInsightService;

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
	private BrowserService browser_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PageService page_service;
	
	@Autowired
	private ElementStateService element_state_service;

	@Autowired
	private PageSpeedAuditService audit_service;
	
	@Autowired
	private AuditDetailService audit_detail_service;
	
	@Autowired
	private PerformanceInsightService performance_insight_service;
	
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
					/*
					Timeout timeout = Timeout.create(Duration.ofSeconds(120));
					Future<Object> future = Patterns.ask(message.getDomainActor(), new DiscoveryActionRequest(message.getDomain(), message.getAccountId()), timeout);
					DiscoveryAction discovery_action = (DiscoveryAction) Await.result(future, timeout.duration());
					
					if(discovery_action == DiscoveryAction.STOP) {
						log.warn("path message discovery actor returning");
						return;
					}
					*/
					
					Page page = browser_service.buildPage(message.getAccountId(), message.getUrl().toString());
					log.warn("page returned :: "+page);
					page = page_service.saveForUser(message.getAccountId(), page);
					
					//log.warn("page states count :: " + page.getPageStates().size());
					PagespeedApiPagespeedResponseV5 page_speed_response = getPageInsights(page.getUrl());
					log.warn("page speed response length :: " + page_speed_response.toPrettyString().length());
					
					PerformanceInsight performance_insight = extractInsights(message.getAccountId(), page_speed_response);
					
					//Page page = new Page(page.getUrl());
					page.setPerformanceScore(performance_insight.getSpeedScore());
					page.setAccessibilityScore(performance_insight.getAccessibilityScore());
					page.setSeoScore(performance_insight.getSeoScore());
					page.setOverallScore(performance_insight.getOverallScore());
					page = page_service.saveForUser(message.getAccountId(), page);
					
					//domain_service.addPageState(message.getDomain().getUrl(), page_state, message.getAccount());
					performance_insight_service.save(performance_insight);
					page_service.addPerformanceInsight(message.getAccountId(), message.getDomain().getUrl(), page.getKey(), performance_insight.getKey());
					domain_service.addPage(message.getDomain().getUrl(), page, message.getAccountId());

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
	private PerformanceInsight extractInsights(String user_id, PagespeedApiPagespeedResponseV5 page_speed_response) {
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

    		PageSpeedAudit audit = new PageSpeedAudit(
    				audit_record.getId(),
    				audit_record.getDescription(),
    				audit_record.getDisplayValue(),
    				audit_record.getErrorMessage(),
    				audit_record.getExplanation(),
    				audit_record.getNumericValue(),
    				audit_record.getScoreDisplayMode(),
    				audit_record.getTitle());
    		Double score = convertScore(audit_record.getScore());
    		audit.setScore(score);
    		audit.setType(insight_type);
    		audit = audit_service.save(audit);
    		
    		speed_insight.addAudit(audit);
    	}
    	
    	double speed_score = convertScore(page_speed_response.getLighthouseResult().getCategories().getPerformance().getScore());
    	double accessibility_score = convertScore(page_speed_response.getLighthouseResult().getCategories().getAccessibility().getScore());
    	double seo_score = convertScore(page_speed_response.getLighthouseResult().getCategories().getSeo().getScore());
    	speed_insight.setSeoScore(seo_score);
    	speed_insight.setAccessibilityScore(accessibility_score);
    	speed_insight.setSpeedScore(speed_score);
    	speed_insight.setOverallScore(calculatePageScore(speed_insight));
    	log.warn("speed insight audits found :: "+speed_insight.getAudits().size());
    	return performance_insight_service.save(speed_insight);
	}

	/**
	 * 
	 * @param performance_insight
	 * @return
	 * 
	 * @pre performance_insight != null
	 */
	private double calculatePageScore(PerformanceInsight performance_insight) {
		assert performance_insight != null;
		
		int insight_cnt = 0;
		double score_total = 0.0;
		
		if(performance_insight.getAccessibilityScore() > 0.0) {
			score_total += performance_insight.getAccessibilityScore();
			insight_cnt++;
		}
		
		log.warn("accessibility score :: " + performance_insight.getAccessibilityScore());
		if(performance_insight.getSpeedScore() > 0.0) {
			score_total += performance_insight.getSpeedScore();
			insight_cnt++;
		}
		log.warn("speed score :: "+performance_insight.getSpeedScore());
		return score_total/insight_cnt;
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
				ArrayMap<String, Object> json_obj = (ArrayMap)obj.get("node");
				String explanation = json_obj.get("explanation").toString();
				String snippet = json_obj.get("snippet").toString();
				snippet = snippet.replaceAll(">\\s+<","> <");
				snippet = snippet.replace("\n", "");
				snippet = snippet.replace("required=\"\"", "required");
				ElementState element_state = element_state_service.findByOuterHtml(user_id, snippet);

				int fix_all_idx = explanation.indexOf("Fix all of the following:");
				int fix_any_idx = explanation.indexOf("Fix any of the following:");
				explanation = explanation.trim();
				
				String optional_details = null;
				String required_details = null;
				if(fix_all_idx > 0 && fix_any_idx == 0) {
					optional_details = explanation.substring(0, fix_all_idx);
					required_details = explanation.substring(fix_all_idx);
				}
				else if(fix_all_idx == 0 && fix_any_idx > 0) {
					required_details = explanation.substring(0, fix_all_idx);
					optional_details = explanation.substring(fix_any_idx);
				}
				else {
					if(fix_all_idx >= 0) {
						required_details = explanation;
						optional_details = "";
					}
					else if(fix_any_idx >= 0) {
						optional_details = explanation;
						required_details = "";
					}
				}

				String[] optional_change_messages = optional_details.split("\\n");
				String[] required_change_messages = required_details.split("\\n");

				AccessibilityDetailNode detail = new AccessibilityDetailNode();
				detail.setOptionalChangeMessages(optional_change_messages);
				detail.setRequiredChangeMessages(required_change_messages);
				detail = (AccessibilityDetailNode)audit_detail_service.save(detail);
				detail.setElement(element_state);
				detail = (AccessibilityDetailNode)audit_detail_service.save(detail);
				audit_details.add(detail);
			}
		}
		
		return audit_details;
	}
	
	/**
	 * Extracts the details object list from the JSON object and formats the data into the appropriate details object
	 * 
	 * @param user_id
	 * @param name
	 * @param details
	 *
	 * @return
	 * 
	 * @pre details != null;
	 */
	private List<AuditDetail> extractAuditDetails(String user_id, String name, Map<String, Object> details) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert name != null;
		assert !name.isEmpty();
		assert details != null;
		
		List<AuditDetail> audit_details = new ArrayList<>();
		if(details != null) {
			List<Object> items = (List<Object>)details.get("items");
			if(items == null) {
				return audit_details;
			}
			for(Object item: items) {
				ArrayMap<String, Object> detail_obj = (ArrayMap)item;

				if("screenshot-thumbnails".contentEquals(name) || "final-screenshot".contentEquals(name)){
					AuditDetail audit_detail = new ScreenshotThumbnailDetails(getDoubleValue(detail_obj, "timing"),
																			  Long.valueOf(detail_obj.get("timestamp").toString()),
																			  detail_obj.get("data").toString());
					audit_details.add(audit_detail);
				}
				else if("resource-summary".contentEquals(name)) {
					AuditDetail audit_detail = new ResourceSummary(getIntegerValue(detail_obj, "requestCount"), 
																   getStringValue(detail_obj, "resourceType"), 
																   getStringValue(detail_obj, "label"), 
																   getLongValue(detail_obj, "size"));
					audit_details.add(audit_detail);
				}
				else if("render-blocking-resources".contentEquals(name)) {
					AuditDetail audit_detail = new BlockingResource(getStringValue(detail_obj, "url"), 
							 										getIntegerValue(detail_obj, "totalBytes"), 
							 										getDoubleValue(detail_obj, "wastedMs"));
					audit_details.add(audit_detail);
				}
				else if("font-display".contentEquals(name)) {
					AuditDetail audit_detail = new BlockingResource(getStringValue(detail_obj, "url"), null, Double.parseDouble(detail_obj.get("wastedMs").toString()));
					audit_details.add(audit_detail);
				}
				else if("user-timings".contentEquals(name) || "main-thread-tasks".contentEquals(name)) {
					AuditDetail audit_detail = new TimingDetail(getStringValue(detail_obj, "name"), 
																getDoubleValue(detail_obj, "startTime"), 
															    getDoubleValue(detail_obj, "duration"), 
															    getStringValue(detail_obj, "timingType"));
					audit_details.add(audit_detail);
				}
				else if("bootup-time".contentEquals(name)) {
					AuditDetail audit_detail = new BootUpTime(detail_obj.get("url").toString(), 
															  getDoubleValue(detail_obj, "total"),
															  getDoubleValue(detail_obj, "scripting"),
															  getDoubleValue(detail_obj, "scriptParseCompile"));
					audit_details.add(audit_detail);
				}
				else if("total-byte-weight".contentEquals(name)) {
					AuditDetail audit_detail = new AssetSize(detail_obj.get("url").toString(), 
															 Integer.parseInt(detail_obj.get("totalBytes").toString()));
					audit_details.add(audit_detail);
				}
				else if("mainthread-work-breakdown".contentEquals(name)) {
					AuditDetail audit_detail = new GroupWorkBreakdown(detail_obj.get("group").toString(), 
																	  getDoubleValue(detail_obj, "duration"), 
																	  detail_obj.get("groupLabel").toString());
					audit_details.add(audit_detail);
				}
				else if("color-contract".contentEquals(name) 
						|| "frame-title".contentEquals(name) 
						|| "link-name".contentEquals(name) 
						|| "meta-viewport".contentEquals(name) 
						|| "label".contentEquals(name)
				) {
					ElementState element_state = element_state_service.findByOuterHtml(user_id,  getStringValue(detail_obj, "snippet"));
					String explanation = getStringValue(detail_obj, "explanation");

					int fix_all_idx = explanation.indexOf("Fix all of the following:");
					int fix_any_idx = explanation.indexOf("Fix any of the following:");
					explanation = explanation.trim();
					
					String optional_details = null;
					String required_details = null;
					if(fix_all_idx > 0 && fix_any_idx == 0) {
						optional_details = explanation.substring(0, fix_all_idx);
						required_details = explanation.substring(fix_all_idx);
					}
					else if(fix_all_idx == 0 && fix_any_idx > 0) {
						required_details = explanation.substring(0, fix_all_idx);
						optional_details = explanation.substring(fix_any_idx);
					}
					else {
						if(fix_all_idx >= 0) {
							required_details = explanation;
							optional_details = "";
						}
						else if(fix_any_idx >= 0) {
							optional_details = explanation;
							required_details = "";
						}
					}

					String[] optional_change_messages = optional_details.split("\\n");
					String[] required_change_messages = required_details.split("\\n");
					
					AuditDetail audit_detail = new AccessibilityDetailNode(required_change_messages, optional_change_messages, element_state);
					audit_details.add(audit_detail);
				}
				else if("dom-size".contentEquals(name)) {
					AuditDetail audit_detail = new DomSize(getStringValue(detail_obj, "statistic"), getStringValue(detail_obj, "value"), (Map)detail_obj.get("element"));
					audit_details.add(audit_detail);
				}
				else if("metrics".contentEquals(name)) {
					AuditDetail audit_detail = new MetricsDetail(getIntegerValue(detail_obj, "firstContentfulPaint"),
																 getLongValue(detail_obj, "observedFirstPaintTs"),
																 getIntegerValue(detail_obj, "speedIndex"),
																 getLongValue(detail_obj, "observedSpeedIndexTs"),
																 getIntegerValue(detail_obj, "observedFirstContentfulPaint"),
																 getLongValue(detail_obj, "observedNavigationStartTs"),
																 getLongValue(detail_obj, "observedLargestContentfulPaintTs"),
																 getIntegerValue(detail_obj, "observedFirstVisualChange"),
																 getLongValue(detail_obj, "observedLoadTs"),
																 getIntegerValue(detail_obj, "firstMeaningfulPaint"),
																 getIntegerValue(detail_obj, "observedTraceEnd"),
																 getIntegerValue(detail_obj, "observedFirstMeaningfulPaint"),
																 getIntegerValue(detail_obj, "firstCPUIdle"),
																 getLongValue(detail_obj, "observedTraceEndTs"),
																 getLongValue(detail_obj, "observedFirstMeaningfulPaintTs"),
																 getIntegerValue(detail_obj, "observedDomContentLoaded"),
																 getLongValue(detail_obj, "observedFirstVisualChangeTs"),
																 getIntegerValue(detail_obj, "interactive"),
																 getIntegerValue(detail_obj, "observedNavigationStart"),
																 getLongValue(detail_obj, "observedFirstContentfulPaintTs"),
																 getLongValue(detail_obj, "observedLastVisualChangeTs"),
																 getIntegerValue(detail_obj, "observedLoad"),
																 getIntegerValue(detail_obj, "observedLargestContentfulPaint"),
																 getLongValue(detail_obj, "observedDomContentLoadedTs"),
																 getIntegerValue(detail_obj, "observedSpeedIndex"),
																 getIntegerValue(detail_obj, "estimatedInputLatency"),
																 getIntegerValue(detail_obj, "totalBlockingTime"),
																 getIntegerValue(detail_obj, "observedFirstPaint"),
																 getIntegerValue(detail_obj, "observedLastVisualChange"),
																 getBooleanValue(detail_obj, "lcpInvalidated"));
					audit_details.add(audit_detail);
				}
				else if("uses-long-cache-ttl".contentEquals(name)) {
					//if debug data is null this might create issues
					AuditDetail audit_detail = new CachingDetail(getStringValue(detail_obj, "url"), 
																 getDoubleValue(detail_obj, "wastedBytes"),
																 getIntegerValue(detail_obj, "totalBytes"),
																 getDoubleValue(detail_obj, "cacheHitProbability"),
																 getLongValue(detail_obj, "cacheLifetimeMs"));
					audit_details.add(audit_detail);
				}
				else if("unminified-css".contentEquals(name) || "unused-css-rules".contentEquals(name) || "offscreen-images".contentEquals(name) || "uses-responsive-images".contentEquals(name)) {
					AuditDetail audit_detail = new AssetSizeOpportunityDetail(detail_obj.get("url").toString(), Integer.parseInt(detail_obj.get("wastedBytes").toString()), Double.parseDouble(detail_obj.get("wastedPercent").toString()), Integer.parseInt(detail_obj.get("totalBytes").toString()));
					audit_details.add(audit_detail);
				}
				else if("third-party-summary".contentEquals(name)) {
					AuditDetail audit_detail = new ThirdPartySummaryDetail(Integer.parseInt(detail_obj.get("transferSize").toString()), 
																		   Double.parseDouble(detail_obj.get("blockingTime").toString()),
																		   Double.parseDouble(detail_obj.get("mainThreadTime").toString()),
																		   (Map)detail_obj.get("entity"));
					audit_details.add(audit_detail);
				}
				else if("network-requests".contentEquals(name)) {
					AuditDetail audit_detail = new NetworkRequestDetail(Integer.parseInt(detail_obj.get("transferSize").toString()), 
																		getStringValue(detail_obj, "url"),
																		getIntegerValue(detail_obj, "statusCode"), 
																		getStringValue(detail_obj, "resourceType"),
																		getStringValue(detail_obj, "mimeType"),
																		getIntegerValue(detail_obj, "resourceSize"),
																		getDoubleValue(detail_obj, "endTime"),
																		getDoubleValue(detail_obj, "startTime"));
					audit_details.add(audit_detail);
				}
				else if("uses-webp-images".contentEquals(name)) {
					AuditDetail audit_detail = new WebPImageDetail(Integer.parseInt(detail_obj.get("wastedBytes").toString()), 
																   detail_obj.get("url").toString(), 
																   Boolean.getBoolean(detail_obj.get("fromProtocol").toString()), 
																   Boolean.getBoolean(detail_obj.get("isCrossOrigin").toString()), 
																   Integer.parseInt(detail_obj.get("totalBytes").toString()));
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
			}
		}
	
		return audit_details;
	}

	private Integer getIntegerValue(ArrayMap<String, Object> detail_obj, String object_key) {
		Integer value = null;
		if(detail_obj.get(object_key) != null) {
			value = Integer.parseInt(detail_obj.get(object_key).toString());
		}
		else {
			value = Integer.MIN_VALUE;
		}
		return value;
	}
	
	private Boolean getBooleanValue(ArrayMap<String, Object> detail_obj, String object_key) {
		Boolean value = null;
		if(detail_obj.get(object_key) != null) {
			value = Boolean.parseBoolean(detail_obj.get(object_key).toString());
		}
		else {
			value = null;
		}
		return value;
	}
	
	private Long getLongValue(ArrayMap<String, Object> detail_obj, String object_key) {
		Long value = null;
		if(detail_obj.get(object_key) != null) {
			value = Long.parseLong(detail_obj.get(object_key).toString());
		}
		else {
			value = Long.MIN_VALUE;
		}
		return value;
	}
	
	private Double getDoubleValue(ArrayMap<String, Object> detail_obj, String object_key) {
		Double value = null;
		if(detail_obj.get(object_key) != null) {
			value = Double.parseDouble(detail_obj.get(object_key).toString());
		}
		else {
			value = Double.MIN_VALUE;
		}
		return value;
	}

	private String getStringValue(ArrayMap<String, Object> detail_obj, String object_key) {
		String mime_type = null;
		if(detail_obj.get(object_key) != null) {
			mime_type = detail_obj.get(object_key).toString();
		}
		
		return mime_type;
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
			score = Double.parseDouble(score_obj.toString());
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
	    Pagespeedonline p = new Pagespeedonline.Builder(transport, jsonFactory, httpRequestInitializer).setApplicationName("Qanairy-Selenium").build();
	    
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
