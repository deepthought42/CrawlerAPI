package com.looksee.api;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.looksee.api.exception.MissingSubscriptionException;
import com.looksee.dto.DomainDto;
import com.looksee.dto.PageStatisticDto;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.DomainSettings;
import com.looksee.models.Element;
import com.looksee.models.PageState;
import com.looksee.models.TestUser;
import com.looksee.models.UXIssueReportDto;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditStats;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.DomainAuditStats;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.SimpleScore;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.repository.TestUserRepository;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.DomainDtoService;
import com.looksee.services.DomainService;
import com.looksee.services.DomainSettingsService;
import com.looksee.services.ReportService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.AuditUtils;
import com.looksee.utils.BrowserUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 * API endpoints for interacting with {@link Domain} data
 */
@Controller
@RequestMapping("/domains")
public class DomainController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AuditRecordService audit_record_service;

	@Autowired
	private AccountService account_service;

	@Autowired
	private DomainService domain_service;

	@Autowired
	private AuditService audit_service;

	@Autowired
	private UXIssueMessageService ux_issue_service;

	@Autowired
	private DomainSettingsService domain_settings_service;
	
	@Autowired
	private ActorSystem actor_system;

	@Autowired
	private DomainDtoService domain_dto_service;
	
	@Autowired
	private TestUserRepository test_user_repo;

	/**
	 * Create a new {@link Domain domain}
	 * 
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	// @PreAuthorize("hasAuthority('write:domains')")
	@RequestMapping(method = RequestMethod.POST)
	public @ResponseBody Domain create(HttpServletRequest request, 
									   @RequestBody(required = true) Domain domain)
			throws UnknownAccountException, MalformedURLException {

		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		log.warn("user id  :: " + id);
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			log.warn("account not found");
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		String lowercase_url = domain.getUrl().toLowerCase();

		log.warn("domain url ::   " + lowercase_url);
		String formatted_url = BrowserUtils.sanitizeUserUrl(lowercase_url);
		log.warn("sanitized domain url ::   " + formatted_url);
		domain.setUrl(formatted_url.replace("http://", "").replace("www.", ""));

		try {
			log.warn("Account email :: " + acct.getEmail());
			log.warn("domain url :: " + domain.getUrl());
			Domain domain_record = account_service.findDomain(acct.getEmail(), domain.getUrl());
			if (domain_record == null) {
				//set default settings
				DomainSettings domain_settings = new DomainSettings("general", "neutral");
				domain.setSettings(domain_settings_service.save(domain_settings));
				domain = domain_service.save(domain);
				account_service.addDomainToAccount(acct, domain);
			} else {
				throw new ExistingAccountDomainException();
			}
		} catch (Exception e) {
			domain = null;
		}

		try {
			MessageBroadcaster.sendDomainAdded(acct.getUserId(), domain);
		} catch (JsonProcessingException e) {
			log.error("Error occurred while sending domain message to user");
		}
		return domain;
	}

	/**
	 * Create a new {@link Domain domain}
	 * 
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	// @PreAuthorize("hasAuthority('write:domains')")
	@RequestMapping(method = RequestMethod.PUT)
	public @ResponseBody Domain update(HttpServletRequest request,
			@RequestParam(value = "key", required = true) String key,
			@RequestParam(value = "protocol", required = true) String protocol,
			@RequestParam(value = "browser_name", required = true) String browser_name,
			@RequestParam(value = "logo_url", required = false) String logo_url

	) throws UnknownAccountException, MalformedURLException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		Domain domain = domain_service.findByKey(key, acct.getEmail());
		domain.setLogoUrl(logo_url);

		return domain_service.save(domain);
	}

	/**
	 * Create a new {@link Domain domain}
	 * 
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	// @PreAuthorize("hasAuthority('write:domains')")
	@RequestMapping(path = "/select", method = RequestMethod.PUT)
	public @ResponseBody void selectDomain(HttpServletRequest request, @RequestBody Domain domain)
			throws UnknownAccountException, MalformedURLException {

		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		acct.setLastDomain(domain.getUrl());
		account_service.save(acct);
	}

    
    /**
     * Update expertise setting in domain settings
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     * @throws MalformedURLException 
     */
    @RequestMapping(method= RequestMethod.POST, path="/{domain_id}/settings/expertise")
    public @ResponseBody DomainSettings updateExpertise(
    		HttpServletRequest request,
    		@PathVariable("domain_id") long domain_id,
    		@RequestBody String expertise
	) throws MalformedURLException {
    	log.warn("domain record id :: "+ domain_id);
    	log.warn("education level :: "+expertise);
    	//Get domain
    	return domain_service.updateExpertiseSettings(domain_id, expertise);
    }
    
    
	// @PreAuthorize("hasAuthority('read:domains')")
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Set<DomainDto> getAll(HttpServletRequest request) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();

		Account acct = account_service.findByUserId(id);
		if (acct == null) {
			log.warn("unknown account...");
			throw new UnknownAccountException();
		}
		/*
		 * else if(acct.getSubscriptionToken() == null){ throw new
		 * MissingSubscriptionException(); }
		 */

		Set<Domain> domains = account_service.getDomainsForAccount(acct.getId());
		Set<DomainDto> domain_info_set = new HashSet<>();
		for (Domain domain : domains) {
			domain_info_set.add(domain_dto_service.build(domain));
		}
		return domain_info_set;
	}

	/**
	 * Removes domain from the current users account
	 * 
	 * @param key
	 * @param domain
	 * @return
	 * @throws UnknownAccountException
	 */
	// @PreAuthorize("hasAuthority('delete:domains')")
	@RequestMapping(method = RequestMethod.DELETE, path = "/{domain_id}")
	public @ResponseBody void remove(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		account_service.removeDomain(acct.getId(), domain_id);
	}

	/**
	 * Retrieves pages for a given domain from the current users account
	 * 
	 * @param key
	 * @param domain
	 * @return
	 * @throws UnknownAccountException
	 */
	// @PreAuthorize("hasAuthority('read:domains')")
	@RequestMapping(method = RequestMethod.GET, path = "/{domain_id}/pages")
	public @ResponseBody Set<PageStatisticDto> getPages(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		Set<PageStatisticDto> page_stats = new HashSet<>();
		// get latest domain audit record
		Optional<DomainAuditRecord> domain_audit_record = audit_record_service.findMostRecentDomainAuditRecord(domain_id);
		if (!domain_audit_record.isPresent()) {
			throw new DomainAuditsNotFound();
		}
		Set<PageAuditRecord> page_audits = audit_record_service.getAllPageAudits(domain_audit_record.get().getId());
		for (PageAuditRecord page_audit : page_audits) {
			PageState page_state = audit_record_service.getPageStateForAuditRecord(page_audit.getId());
			if (page_state == null) {
				continue;
			}
			
			double content_score = AuditUtils
					.calculateScore(audit_record_service.getAllContentAudits(page_audit.getId()));
			double info_architecture_score = AuditUtils
					.calculateScore(audit_record_service.getAllInformationArchitectureAudits(page_audit.getId()));
			double aesthetic_score = AuditUtils
					.calculateScore(audit_record_service.getAllAestheticAudits(page_audit.getId()));
			double accessibility_score = AuditUtils
					.calculateScore(audit_record_service.getAllAccessibilityAudits(page_audit.getId()));

			PageStatisticDto page = new PageStatisticDto(page_state.getId(), 
														 page_state.getUrl(),
														 page_state.getViewportScreenshotUrl(), 
														 content_score, 
														 page_audit.getContentAuditProgress(),
														 info_architecture_score, 
														 page_audit.getInfoArchitechtureAuditProgress(), 
														 accessibility_score, 
														 0.0,
														 aesthetic_score, 
														 page_audit.getAestheticAuditProgress(), 
														 page_audit.getId(),
														 page_audit.getElementsReviewed(), 
														 page_audit.getElementsFound(),
														 page_audit.isComplete(),
														 page_audit.getDataExtractionProgress());

			page_stats.add(page);
		}

		return page_stats;
	}

	/**
	 * Retrieves that AuditStats for the domain with the given ID
	 * 
	 * @return {@link PerformanceInsight insight}
	 * @throws UnknownAccountException
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/{domain_id}/stats")
	public @ResponseBody AuditStats getAuditStat(HttpServletRequest request, 
												@PathVariable("domain_id") long domain_id)
			throws UnknownAccountException {
		// get most recent audit record for the domain
		Optional<DomainAuditRecord> audit_record_opt = domain_service.getMostRecentAuditRecord(domain_id);

		if (audit_record_opt.isPresent()) {
			AuditRecord audit_record = audit_record_opt.get();
			long content_audits_complete = 0;
			long info_arch_audits_complete = 0;
			long aesthetic_audits_complete = 0;
			long element_extractions_complete = 0;
			
			Set<PageAuditRecord> audit_records = audit_record_service.getAllPageAudits(audit_record.getId());
			// get Page Count
			long page_count = audit_records.size();
			long pages_audited = 0;

			double score = 0.0;
			int audit_count = 0;
			long high_issue_count = 0;
			long mid_issue_count = 0;
			long low_issue_count = 0;

			List<DomainAuditRecord> audit_record_history = domain_service.getAuditRecordHistory(domain_id);
			
			List<SimpleScore> overall_score_history = new ArrayList<>();
			List<SimpleScore> content_score_history = new ArrayList<>();
			List<SimpleScore> info_architecture_score_history = new ArrayList<>();
			List<SimpleScore> aesthetic_score_history = new ArrayList<>();
			List<SimpleScore> accessibility_score_history = new ArrayList<>();

			for(DomainAuditRecord record : audit_record_history) {
				Set<PageAuditRecord> page_audits = record.getAudits();
				
				double overall_score = 0;
				double aesthetic_score = 0;
				double info_architecture_score = 0;
				double content_score = 0;
				log.warn("Reviewing page audits for overall score");
				for(PageAuditRecord page_audit: page_audits) {
					Set<Audit> audits = page_audit.getAudits();
					
					overall_score += (int)( AuditUtils.calculateScore(audits) * 100 );
					aesthetic_score += (int)( AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS) * 100);
					info_architecture_score += (int)( AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE) * 100);
					content_score += (int)( AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT) * 100 );
				}
				
				if(!page_audits.isEmpty()) {					
					overall_score_history.add(new SimpleScore(record.getCreatedAt(), overall_score/page_audits.size() ));				
					aesthetic_score_history.add(new SimpleScore(record.getCreatedAt(), aesthetic_score/page_audits.size() ));
					info_architecture_score_history.add(new SimpleScore(record.getCreatedAt(), info_architecture_score/page_audits.size() ));
					content_score_history.add(new SimpleScore(record.getCreatedAt(), content_score/page_audits.size() ));
				}
				
			}
			
			double content_score = 0.0;
			double written_content_score = 0.0;
			double imagery_score = 0.0;
			double videos_score = 0.0;
			double audio_score = 0.0;
			int written_content_issue_count = 0;
			int imagery_issue_count = 0;
			int video_issue_count = 0;
			int audit_issue_count = 0;
			
			double info_arch_score = 0.0;
			double seo_score = 0.0;
			double menu_analysis_score = 0.0;
			double performance_score = 0.0;
			double link_score = 0.0;
			int seo_issue_count = 0;
			int menu_issue_count = 0;
			int performance_issue_count = 0;
			int link_issue_count = 0;
			
			double aesthetic_score = 0.0;
			//double color_score = 0.0;
			double text_contrast_score = 0.0;
			double non_text_contrast_score = 0.0;
			double typography_score = 0.0;
			double whitespace_score = 0.0;
			double branding_score = 0.0;
			//int color_issue_count = 0;
			int text_contrast_issue_count = 0;
			int non_text_contrast_issue_count = 0;
			int typography_issue_count = 0;
			int whitespace_issue_count = 0;
			int branding_issue_count = 0;
			int total_issues = 0;
			
			long elements_reviewed = 0;
			long elements_found = 0;

			for (PageAuditRecord page_audit : audit_records) {
				if (page_audit.isComplete()) {
					pages_audited++;
				}

				elements_reviewed += page_audit.getElementsReviewed();
				elements_found += page_audit.getElementsFound();

				log.warn("starting get all audits....");
				Set<Audit> audits = audit_record_service.getAllAudits(page_audit.getId());
				log.warn("retreived audits. Tabulating scores now");
				written_content_score += AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.WRITTEN_CONTENT);
				imagery_score += AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.IMAGERY);
				videos_score += 0;//AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.VIDEOS);
				audio_score += 0;//AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.AUDIO);

				written_content_issue_count += audit_service.countAuditBySubcategory(audits, AuditSubcategory.WRITTEN_CONTENT);
				imagery_issue_count += audit_service.countAuditBySubcategory(audits, AuditSubcategory.IMAGERY);
				video_issue_count += 0;//AuditUtils.countAuditBySubcategory(audits, AuditSubcategory.VIDEOS);
				audit_issue_count += 0;//AuditUtils.countAuditBySubcategory(audits, AuditSubcategory.AUDIO);

				seo_issue_count += audit_service.countAuditBySubcategory(audits, AuditSubcategory.SEO);
				menu_issue_count += 0;//AuditUtils.countAuditBySubcategory(audits, AuditSubcategory.MENU_ANALYSIS);
				performance_issue_count += 0;//AuditUtils.countAuditBySubcategory(audits, AuditSubcategory.PERFORMANCE);
				link_issue_count += audit_service.countIssuesByAuditName(audits, AuditName.LINKS);

				seo_score += AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.SEO);
				menu_analysis_score += 0;//AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.MENU_ANALYSIS);
				performance_score += 0;//AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.PERFORMANCE);
				link_score += AuditUtils.calculateScoreByName(audits, AuditName.LINKS);

				//aesthetic_score = AuditUtils.calculateScore(audits);
				//color_issue_count += audit_service.countAuditBySubcategory(audits, AuditSubcategory.COLOR_MANAGEMENT);
				text_contrast_issue_count += audit_service.countIssuesByAuditName(audits, AuditName.TEXT_BACKGROUND_CONTRAST);
				non_text_contrast_issue_count += audit_service.countIssuesByAuditName(audits, AuditName.NON_TEXT_BACKGROUND_CONTRAST);

				typography_issue_count += audit_service.countAuditBySubcategory(audits, AuditSubcategory.TYPOGRAPHY);
				whitespace_issue_count += 0; //AuditUtils.countAuditBySubcategory(audits, AuditSubcategory.WHITESPACE);
				branding_issue_count += 0; //AuditUtils.countAuditBySubcategory(audits, AuditSubcategory.BRANDING);
				
				//color_score += AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.COLOR_MANAGEMENT);
				text_contrast_score += AuditUtils.calculateScoreByName(audits, AuditName.TEXT_BACKGROUND_CONTRAST);
				non_text_contrast_score += AuditUtils.calculateScoreByName(audits, AuditName.NON_TEXT_BACKGROUND_CONTRAST);
				typography_score += AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.TYPOGRAPHY);
				whitespace_score += 0;//AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.WHITESPACE);
				branding_score += 0;//AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.BRANDING);

				high_issue_count += audit_record_service.getIssueCountBySeverity(page_audit.getId(),
						Priority.HIGH.toString());
				mid_issue_count += audit_record_service.getIssueCountBySeverity(page_audit.getId(),
						Priority.MEDIUM.toString());
				low_issue_count += audit_record_service.getIssueCountBySeverity(page_audit.getId(),
						Priority.LOW.toString());

				total_issues = written_content_issue_count
								+ imagery_issue_count
								+ video_issue_count
								+ audit_issue_count
								+ seo_issue_count
								+ menu_issue_count
								+ performance_issue_count
								+ link_issue_count
								+ text_contrast_issue_count
								+ non_text_contrast_issue_count
								+ typography_issue_count
								+ whitespace_issue_count
								+ branding_issue_count;

				for (Audit audit : audits) {
					// get issues
					if (audit.getTotalPossiblePoints() == 0) {
						score += 1;
					} else {
						score += (audit.getPoints() / (double) audit.getTotalPossiblePoints());
					}
					
				}
				audit_count += audits.size();

				if (page_audit.getInfoArchitechtureAuditProgress() >= 1.0) {
					info_arch_audits_complete++;
				}
				if (page_audit.getContentAuditProgress() >= 1.0) {
					content_audits_complete++;
				}
				if (page_audit.getAestheticAuditProgress() >= 1.0) {
					aesthetic_audits_complete++;
				}
				if (page_audit.getDataExtractionProgress() >= 1.0) {
					element_extractions_complete++;
				}
			}

			
			double overall_score = (score / (double) audit_count) * 100.0;

			// build stats object
			AuditStats audit_stats = new DomainAuditStats(audit_record.getId(),
														  audit_record.getStartTime(),
														  audit_record.getEndTime(), 
														  pages_audited, 
														  page_count, 
														  content_audits_complete,
														  content_audits_complete / (double) audit_records.size(),
														  written_content_issue_count,
														  imagery_issue_count,
														  video_issue_count,
														  audit_issue_count,
														  written_content_score / (double) audit_records.size(), 
														  imagery_score / (double) audit_records.size(),
														  videos_score / (double) audit_records.size(), 
														  audio_score / (double) audit_records.size(), 
														  audit_record.getContentAuditMsg(), 
														  info_arch_audits_complete,
														  info_arch_audits_complete / (double) audit_records.size(),
														  seo_issue_count,
														  menu_issue_count,
														  performance_issue_count,
														  link_issue_count,
														  seo_score / (double) audit_records.size(), 
														  menu_analysis_score / (double) audit_records.size(),
														  performance_score / (double) audit_records.size(),
														  link_score / (double) audit_records.size(),
														  audit_record.getInfoArchMsg(),
														  aesthetic_audits_complete,
														  aesthetic_audits_complete / (double) audit_records.size(),
														  text_contrast_issue_count,
														  non_text_contrast_issue_count,
														  typography_issue_count,
														  whitespace_issue_count,
														  branding_issue_count,
														  text_contrast_score / (double) audit_records.size(),
														  non_text_contrast_score / (double) audit_records.size(),
														  typography_score / (double) audit_records.size(),
														  whitespace_score  / (double) audit_records.size(),
														  branding_score / (double) audit_records.size(),
														  audit_record.getAestheticMsg(),
														  overall_score,
														  high_issue_count,
														  mid_issue_count,
														  low_issue_count,
														  elements_reviewed,
														  elements_found,
														  audit_record.getDataExtractionMsg(), 
														  element_extractions_complete / (double) audit_records.size(), 
														  overall_score_history, 
														  content_score_history, 
														  info_architecture_score_history, 
														  aesthetic_score_history, 
														  accessibility_score_history,
														  total_issues);

			return audit_stats;
		} else {
			throw new AuditRecordNotFoundException();
		}
	}

	// @PreAuthorize("hasAuthority('read:domains')")
	@RequestMapping(method = RequestMethod.GET, path = "/pages")
	public @ResponseBody Set<PageState> getAllPages(HttpServletRequest request,
			@RequestParam(value = "url", required = true) String url)
			throws UnknownAccountException, MalformedURLException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName().replace("auth0|", "");
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		URL url_obj = new URL(BrowserUtils.sanitizeUrl(url, false));
		Set<PageState> pages = domain_service.getPages(url_obj.getHost());

		// TODO filter through pages to get most recent for each unique page url
		return pages;
	}

	@SafeVarargs
	public static <T> Set<T> merge(Collection<? extends T>... collections) {
		Set<T> newSet = new HashSet<T>();
		for (Collection<? extends T> collection : collections)
			newSet.addAll(collection);
		return newSet;
	}

	/**
	 * 
	 * @param request
	 * @param host
	 * 
	 * @return a unique set of {@link Element}s belonging to all page states for the
	 *         {@link Domain} with the given host
	 * @throws UnknownAccountException
	 */
	// @PreAuthorize("hasAuthority('read:domains')")
	@RequestMapping(method = RequestMethod.GET, path = "/page_elements")
	public @ResponseBody Set<Element> getAllElementStates(HttpServletRequest request,
			@RequestParam(value = "host", required = true) String host) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName().replace("auth0|", "");
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		Set<Element> page_elements = domain_service.getElementStates(host, acct.getEmail());
		log.info("###### PAGE ELEMENT COUNT :: " + page_elements.size());
		return page_elements;
	}

	/**
	 * Create a new test user and add it to the domain
	 * 
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * 
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	// @PreAuthorize("hasAuthority('create:domains')")
	@RequestMapping(path = "/{domain_id}/users", method = RequestMethod.POST)
	public @ResponseBody TestUser addUser(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id,
			@RequestParam(value = "username", required = true) String username,
			@RequestParam(value = "password", required = true) String password,
			@RequestParam(value = "role", required = false) String role,
			@RequestParam(value = "enabled", required = true) boolean enabled)
			throws UnknownAccountException, MalformedURLException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName().replace("auth0|", "");
		Account account = account_service.findByUserId(id);

		if (account == null) {
			throw new UnknownAccountException();
		}

		Optional<Domain> optional_domain = domain_service.findById(domain_id);

		log.info("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
		log.info("starting to add user");
		if (optional_domain.isPresent()) {
			Domain domain = optional_domain.get();
			log.info("domain : " + domain);
			Set<TestUser> test_users = domain_service.getTestUsers(account.getEmail(), domain.getKey());

			log.info("Test users : " + test_users.size());
			for (TestUser user : test_users) {
				if (user.getUsername().equals(username)) {
					log.info("User exists, returning user : " + user);
					return user;
				}
			}

			log.info("Test user does not exist for domain yet");

			TestUser user = new TestUser(username, password, role, enabled);
			user = test_user_repo.save(user);
			Set<TestUser> users = new HashSet<TestUser>();
			users.add(user);
			domain.setTestUsers(users);
			domain = domain_service.save(domain);
			log.info("saved domain :: " + domain.getKey());
			return user;
		}
		throw new DomainNotFoundException();
	}

	/**
	 * Get Excel file for domain with the given id
	 * 
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * 
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	// @PreAuthorize("hasAuthority('create:domains')")
	@RequestMapping(path = "/{domain_id}/excel", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<Resource> exportExcelReport(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id)
			throws UnknownAccountException, FileNotFoundException, IOException {
		Optional<Domain> domain_opt = domain_service.findById(domain_id);
		if (!domain_opt.isPresent()) {
			throw new DomainNotFoundException();
		}

		Optional<DomainAuditRecord> domain_audit = domain_service.getMostRecentAuditRecord(domain_opt.get().getId());
		if (!domain_audit.isPresent()) {
			throw new DomainAuditsNotFound();
		}

		List<UXIssueReportDto> ux_issues = new ArrayList<>();
		Set<PageAuditRecord> page_audits = audit_record_service.getAllPageAudits(domain_audit.get().getId());
		for (PageAuditRecord page_audit : page_audits) {
			Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(page_audit.getId());
			PageState page = audit_record_service.getPageStateForAuditRecord(page_audit.getId());
			for (Audit audit : audits) {
				log.warn("audit key :: " + audit.getKey());
				Set<UXIssueMessage> messages = audit_service.getIssues(audit.getId());
				log.warn("audit issue messages size ...." + messages.size());

				for (UXIssueMessage message : messages) {
					String element_selector = "";
					if (ObservationType.ELEMENT.equals(message.getType())
							|| ObservationType.COLOR_CONTRAST.equals(message.getType())) {
						element_selector = ux_issue_service.getElement(message.getId()).getCssSelector();
					} else {
						element_selector = "No specific element is associated with this issue";
					}

					UXIssueReportDto issue_dto = new UXIssueReportDto(message.getRecommendation(),
							message.getPriority(), message.getDescription(), message.getType(), message.getCategory(),
							message.getWcagCompliance(), message.getLabels(), audit.getWhyItMatters(),
							message.getTitle(), element_selector, page.getUrl());
					ux_issues.add(issue_dto);
				}

			}
			log.warn("UX audits :: " + ux_issues.size());
		}
		URL sanitized_domain_url = new URL(BrowserUtils.sanitizeUrl(domain_opt.get().getUrl(), false));
		XSSFWorkbook workbook = ReportService.generateDomainExcelSpreadsheet(ux_issues, sanitized_domain_url);

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			workbook.write(outputStream);

			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Disposition", "attachment; " + sanitized_domain_url.getHost() + ".xlsx");

			return ResponseEntity.ok()
					.contentType(MediaType
							.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
					.cacheControl(CacheControl.noCache()).headers(headers)
					.body(new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray())));
		}
	}

	/**
	 * 
	 * @param request
	 * @param user_id
	 * @throws UnknownAccountException
	 * @throws UnknownUserException
	 */
	// @PreAuthorize("hasAuthority('create:test_user')")
	@RequestMapping(path = "test_users/$user_id", method = RequestMethod.DELETE)
	public @ResponseBody void delete(HttpServletRequest request,
			@RequestParam(value = "domain_key", required = true) String domain_key,
			@RequestParam(value = "username", required = true) String username) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName().replace("auth0|", "");
		Account account = account_service.findByUserId(id);

		if (account == null) {
			throw new UnknownAccountException();
		}

		domain_service.deleteTestUser(account.getEmail(), domain_key, username);
	}

	/**
	 * 
	 * @param request
	 * @param domain_id
	 * @return
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	// @PreAuthorize("hasAuthority('create:domains')")
	@RequestMapping(path = "{domain_id}/users", method = RequestMethod.GET)
	public @ResponseBody Set<TestUser> getUsers(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id)
			throws UnknownAccountException, MalformedURLException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account account = account_service.findByUserId(id);

		if (account == null) {
			throw new UnknownAccountException();
		}

		Optional<Domain> optional_domain = domain_service.findById(domain_id);
		if (optional_domain.isPresent()) {
			Domain domain = optional_domain.get();
			Set<TestUser> users = domain_service.getTestUsers(account.getEmail(), domain.getKey());

			return users;
		} else {
			throw new DomainNotFoundException();
		}
	}

	/**
	 * Starts an audit on an entire domain
	 * 
	 * @param request
	 * @param page
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	// @PreAuthorize("hasAuthority('execute:audits')")
	@RequestMapping(path = "/{domain_id}/start", method = RequestMethod.POST)
	public @ResponseBody AuditRecord startAudit(HttpServletRequest request, 
											@PathVariable("domain_id") long domain_id)
			throws Exception {
		Principal principal = request.getUserPrincipal();
		String user_id = principal.getName();
		Account account = account_service.findByUserId(user_id);

		if (account == null) {
			throw new UnknownAccountException();
		}
		log.warn("looking for domain by id :: " + domain_id);
		Optional<Domain> domain_opt = domain_service.findById(domain_id);
		if (!domain_opt.isPresent()) {
			throw new DomainNotFoundException();
		}

		Domain domain = domain_opt.get();
		String lowercase_url = domain.getUrl().toLowerCase();
		URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(lowercase_url));

		// create new audit record
		AuditRecord audit_record = new DomainAuditRecord(ExecutionStatus.IN_PROGRESS);
		audit_record.setUrl(domain.getUrl());
		log.warn("audit record found ..." + audit_record.getKey());
		audit_record = audit_record_service.save(audit_record, account.getId(), domain.getId());

		domain_service.addAuditRecord(domain.getId(), audit_record.getKey());
		account_service.addAuditRecord(account.getEmail(), audit_record.getId());

		ActorRef audit_manager = actor_system.actorOf(SpringExtProvider.get(actor_system).props("auditManager"),
				"auditManager" + UUID.randomUUID());
		CrawlActionMessage crawl_action = new CrawlActionMessage(CrawlAction.START, 
																 domain.getId(), 
																 account.getId(),
																 audit_record, 
																 false, 
																 sanitized_url);
		audit_manager.tell(crawl_action, null);

		return audit_record;
	}

	/**
	 * Retrieves list of {@link PerformanceInsight insights} with a given key
	 * 
	 * @param key account key
	 * @return {@link PerformanceInsight insight}
	 * @throws UnknownAccountException
	 */
	// @PreAuthorize("hasAuthority('read:actions')")
	@RequestMapping(method = RequestMethod.GET, path = "/audits")
	public DomainAuditRecord getMostRecentDomainAuditRecord(HttpServletRequest request,
			@PathVariable(value = "host", required = true) String host) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		}

		log.info("finding all page insights :: " + host);
		return domain_service.getMostRecentAuditRecord(host).get();
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class RequiredFieldMissingException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public RequiredFieldMissingException() {
		super("Please fill in or select all required fields.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class QanairyEmployeesOnlyException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public QanairyEmployeesOnlyException() {
		super("It looks like you tried to add a Qanairy domain. If you would like to test Qanairy, please apply by emailing us at careers@qanairy.com.");
	}
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class DomainNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3490016388183179302L;

	public DomainNotFoundException() {
		super("Domain could not be found.");
	}
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class FormNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6272761200954686735L;

	public FormNotFoundException() {
		super("Form could not be found.");
	}
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class DomainAuditsNotFound extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7815442042430032220L;

	public DomainAuditsNotFound() {
		super("No audits were found for this domain");
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class ExistingAccountDomainException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8549092797919036363L;

	public ExistingAccountDomainException() {
		super("This domain already exists for your account.");
	}
}
