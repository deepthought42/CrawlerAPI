package com.crawlerApi.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
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

import com.crawlerApi.api.exception.MissingSubscriptionException;
import com.crawlerApi.browsing.Crawler;
import com.crawlerApi.dto.AuditRecordDto;
import com.crawlerApi.generators.report.GeneratePDFReport;
import com.crawlerApi.models.Account;
import com.crawlerApi.models.ElementState;
import com.crawlerApi.models.PageState;
import com.crawlerApi.models.UXIssueReportDto;
import com.crawlerApi.models.audit.Audit;
import com.crawlerApi.models.audit.AuditRecord;
import com.crawlerApi.models.audit.UXIssueMessage;
import com.crawlerApi.models.audit.performance.PerformanceInsight;
import com.crawlerApi.models.designsystem.DesignSystem;
import com.crawlerApi.models.dto.exceptions.UnknownAccountException;
import com.crawlerApi.models.enums.AuditCategory;
import com.crawlerApi.models.enums.AuditName;
import com.crawlerApi.models.enums.AuditSubcategory;
import com.crawlerApi.models.enums.ObservationType;
import com.crawlerApi.models.enums.WCAGComplianceLevel;
import com.crawlerApi.security.SecurityConfig;
import com.crawlerApi.services.AccountService;
import com.crawlerApi.services.AuditRecordService;
import com.crawlerApi.services.AuditService;
import com.crawlerApi.services.ElementStateService;
import com.crawlerApi.services.PageStateService;
import com.crawlerApi.services.ReportService;
import com.crawlerApi.services.UXIssueMessageService;
import com.crawlerApi.utils.AuditUtils;
import com.crawlerApi.utils.BrowserUtils;
import com.crawlerApi.utils.ContentUtils;
import com.crawlerApi.utils.PDFDocUtils;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping(path = "audits", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuditController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

   	public final static long SECS_PER_HOUR = 60 * 60;
	
	@Autowired
	private AccountService account_service;
    
    @Autowired
    protected SecurityConfig appConfig;
    
    @Autowired
    protected AuditService audit_service;
    
    @Autowired
    protected UXIssueMessageService issue_message_service;
    
    @Autowired
    protected PageStateService page_state_service;
    
    @Autowired
    protected ElementStateService element_state_service;
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
	@Autowired
	private UXIssueMessageService ux_issue_service;
    
    /**
     * Retrieves list of audits {@link Audit audits} from last 30 days
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws MalformedURLException 
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody List<AuditRecordDto> getAudits(HttpServletRequest request)
		throws MalformedURLException, UnknownAccountException
    {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);
		
		if(acct == null){
			throw new UnknownAccountException();
		}
		else if(acct.getSubscriptionToken() == null){
			throw new MissingSubscriptionException();
		}

		List<AuditRecord> audits_records = audit_record_service.findByAccountId(acct.getId());
		return audit_record_service.buildAudits(audits_records);
    }

	/**
     * Retrieves {@link Audit audit} with given ID
     * 
     * @param id
     * @return {@link Audit audit} with given ID
	 * 
	 * @throws UnknownAccountException 
	 * @throws MissingSubscriptionException 
     */
    @RequestMapping(method= RequestMethod.GET, path="/{id}")
    public @ResponseBody List<AuditRecordDto> getAudit(HttpServletRequest request,
									@PathVariable("id") long id) throws MissingSubscriptionException, UnknownAccountException
    {
		account_service.retrieveAndValidateAccount(request.getUserPrincipal());
		List<AuditRecord> audits_records = audit_record_service.getAllPageAudits(id);
		return audit_record_service.buildAudits(audits_records);
    }

	/**
     * Creates a new {@link Observation observation} 
     * 
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.POST, value="/$key/issues")
    public @ResponseBody UXIssueMessage addIssue(
										HttpServletRequest request,
										@PathVariable("key") String key,
										@RequestBody UXIssueMessage issue_message
	) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	

    	//find audit by key
    	//find audit by key and add recommendation
    
    	//add observation to page

    	issue_message.setKey(issue_message.generateKey());
		issue_message = issue_message_service.save( issue_message );
		audit_service.addIssue(key, issue_message.getKey());

		return issue_message;
    }

	
	@RequestMapping("/stop")
	public @ResponseBody void stopAudit(HttpServletRequest request, 
				@RequestParam(value="url", required=true) String url)
			throws MalformedURLException, UnknownAccountException 
	{
	   	Principal principal = request.getUserPrincipal();
	   	String id = principal.getName().replace("auth0|", "");
	   	Account acct = account_service.findByUserId(id);
	
	   	if(acct == null){
	   		throw new UnknownAccountException();
	   	}
	   	else if(acct.getSubscriptionToken() == null){
	   		throw new MissingSubscriptionException();
	   	}
	}
	
	/**
	 * Get Excel file for {@link AuditRecord} with the given id
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * 
	 * @throws UnknownAccountException
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
    @RequestMapping(path="/{audit_id}/report/excel", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Resource> exportExcelReport(HttpServletRequest request,
    									@PathVariable(value="audit_id", required=true) long audit_id) 
    											throws UnknownAccountException, 
														FileNotFoundException, IOException {
    	Optional<AuditRecord> audit_opt = audit_record_service.findById(audit_id);
    	if(!audit_opt.isPresent()) {
    		throw new AuditRecordNotFoundException();
    	}
    	
    	List<UXIssueReportDto> ux_issues = new ArrayList<>();
		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_opt.get().getId());
		PageState page = audit_record_service.getPageStateForAuditRecord(audit_opt.get().getId());	
    	for(Audit audit : audits) {
    		log.warn("audit key :: "+audit.getKey());
    		Set<UXIssueMessage> messages = audit_service.getIssues(audit.getId());
    		
    		for(UXIssueMessage message : messages) {
    			String element_selector = "";
    			if(ObservationType.ELEMENT.equals(message.getType()) 
					|| ObservationType.COLOR_CONTRAST.equals(message.getType())) {
					ElementState element = ux_issue_service.getElement(audit_id);
					if(element == null){
						continue;
					}
					System.out.println("ux_issue_service.getElement(message.getId()) = "+ux_issue_service.getElement(message.getId()));
    				element_selector = element.getCssSelector();
    			}
    			else {
    				element_selector = "No specific element is associated with this issue";
    			}
    			
    			UXIssueReportDto issue_dto = new UXIssueReportDto(message.getRecommendation(),
    															  message.getPriority(),
    															  message.getDescription(),
    															  message.getType(),
    															  message.getCategory(),
    															  message.getWcagCompliance(),
    															  message.getLabels(),
    															  audit.getWhyItMatters(),
    															  message.getTitle(),
    															  element_selector,
    															  page.getUrl());
    			ux_issues.add(issue_dto);
    		}
    		
    	}

    	URL sanitized_domain_url = new URL(BrowserUtils.sanitizeUrl(page.getUrl(), page.isSecured()));
    	XSSFWorkbook workbook = ReportService.generateExcelSpreadsheet(ux_issues, sanitized_domain_url);
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);

    		HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; " + sanitized_domain_url.getHost()+".xlsx");
            
            return ResponseEntity.ok()
            		.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            		.cacheControl(CacheControl.noCache())
            		.headers(headers)
            		.body(new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray())));
        }
    }

	/**
	 * Get Excel file for {@link AuditRecord} with the given id
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * 
	 * @throws UnknownAccountException
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
    @RequestMapping(path="/{audit_id}/report/pdf", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Resource> exportPDFReport(HttpServletRequest request,
    									@PathVariable(value="audit_id", required=true) long audit_id) 
    											throws UnknownAccountException, 
														FileNotFoundException, IOException {
    	Optional<AuditRecord> audit_opt = audit_record_service.findById(audit_id);
    	if(!audit_opt.isPresent()) {
    		throw new AuditRecordNotFoundException();
    	}
		AuditRecord audit_record = audit_opt.get();
    	
    	List<UXIssueReportDto> ux_issues = new ArrayList<>();
		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_opt.get().getId());
		PageState page = audit_record_service.getPageStateForAuditRecord(audit_opt.get().getId());	
    	for(Audit audit : audits) {
    		log.warn("audit key :: "+audit.getKey());
    		Set<UXIssueMessage> messages = audit_service.getIssues(audit.getId());
    		
    		for(UXIssueMessage message : messages) {
    			String element_selector = "";
    			if(ObservationType.ELEMENT.equals(message.getType()) 
					|| ObservationType.COLOR_CONTRAST.equals(message.getType())) {
					ElementState element = ux_issue_service.getElement(audit_id);
					if(element == null){
						continue;
					}
					System.out.println("ux_issue_service.getElement(message.getId()) = "+ux_issue_service.getElement(message.getId()));
    				element_selector = element.getCssSelector();
    			}
    			else {
    				element_selector = "No specific element is associated with this issue";
    			}
    			
    			UXIssueReportDto issue_dto = new UXIssueReportDto(message.getRecommendation(),
    															  message.getPriority(),
    															  message.getDescription(),
    															  message.getType(),
    															  message.getCategory(),
    															  message.getWcagCompliance(),
    															  message.getLabels(),
    															  audit.getWhyItMatters(),
    															  message.getTitle(),
    															  element_selector,
    															  page.getUrl());
    			ux_issues.add(issue_dto);
    		}
    		
    	}

    	URL sanitized_domain_url = new URL(BrowserUtils.sanitizeUrl(page.getUrl(), page.isSecured()));
    	
		List<AuditSubcategory> needs_improvement = PDFDocUtils.getTopFourCategoriesThatNeedImprovement(audits);
		double overall_score = AuditUtils.calculateScore(audits);
		double aesthetic_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.COLOR_MANAGEMENT);
		double color_palette_score = AuditUtils.calculateScoreByName(audits, AuditName.COLOR_PALETTE);
		double text_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.TEXT_BACKGROUND_CONTRAST);
		double non_text_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		double written_content_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.WRITTEN_CONTENT);
		double paragraphing_score = AuditUtils.calculateScoreByName(audits, AuditName.PARAGRAPHING);
		double visuals_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.IMAGERY);
		double visuals_imagery_score = AuditUtils.calculateScoreByName(audits, AuditName.IMAGE_COPYRIGHT);
		double information_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
		double branding_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.BRANDING);
		double ease_of_understanding_score = AuditUtils.calculateScoreByName(audits, AuditName.READING_COMPLEXITY);
		
		double percentage_of_passing_large_text_items = AuditUtils.getPercentPassingLargeTextItems(audits);
		double percent_failing_large_text_items = 100.0 - percentage_of_passing_large_text_items;
		
		double percent_failing_small_text_items = AuditUtils.getPercentFailingSmallTextItems(audits);
		List<AuditRecord> auditRecords = new ArrayList<AuditRecord>();
		auditRecords.add(audit_record);

		double percentage_pages_non_text_issues = AuditUtils.getCountPagesWithSubcategoryIssues(auditRecords, AuditSubcategory.NON_TEXT_CONTRAST) / (double)audits.size();
		int number_of_pages_paragraphing_issues = AuditUtils.getCountPagesWithIssuesByAuditName(auditRecords, AuditName.PARAGRAPHING);
		
		DesignSystem design_system = new DesignSystem();//domain_service.getDesignSystem(domain.getId()).get();
		WCAGComplianceLevel wcag_company_compliance_level = design_system.getWcagComplianceLevel();
		int non_ada_compliant_pages = AuditUtils.getCountOfPagesWithWcagComplianceIssues(auditRecords);
		double average_words_per_sentence = AuditUtils.calculateAverageWordsPerSentence(audits);
		double stock_image_percentage = AuditUtils.calculatePercentStockImages(audits);
		double percent_custom_images = 100.0 - stock_image_percentage;
		log.warn("audit count = "+audits.size());
		double avg_reading_complexity = AuditUtils.calculateAverageReadingComplexity(audits);
		log.warn("average reading complexity = "+avg_reading_complexity);
		log.warn("target user education = "+audit_record.getTargetUserEducation());
		String avg_difficulty_string = ContentUtils.getReadingDifficultyRatingByEducationLevel(avg_reading_complexity, 
																								audit_record.getTargetUserEducation());
		log.warn("average difficulty = "+avg_difficulty_string);
		String avg_grade_level = ContentUtils.getReadingGradeLevel(avg_reading_complexity);
		
		GeneratePDFReport pdf_report = null;
		try {
			pdf_report = new GeneratePDFReport(page.getUrl());
		
			pdf_report.writeDocument(needs_improvement, 
									page.getUrl(), 
									audits.size(), 
									(int)overall_score,
									(int)aesthetic_score, 
									(int)color_palette_score, 
									(int)text_contrast_score,
									(int)percentage_of_passing_large_text_items, 
									(int)percent_failing_large_text_items, 
									(int)percent_failing_small_text_items, 
									(int)non_text_contrast_score, 
									(int)percentage_pages_non_text_issues, 
									(int)written_content_score, 
									(int)ease_of_understanding_score, 
									(int)paragraphing_score, 
									(int)number_of_pages_paragraphing_issues, 
									(int)average_words_per_sentence, 
									(int)visuals_score, 
									(int)visuals_imagery_score, 
									(int)percent_custom_images, 
									(int)stock_image_percentage, 
									wcag_company_compliance_level, 
									(int)information_architecture_score, 
									(int)branding_score, 
									audit_record.getColors(),
									avg_difficulty_string,
									avg_grade_level, 
									non_ada_compliant_pages);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			//return error response with error "URI is invalid"
		}

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			pdf_report.write(outputStream);
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Disposition", "attachment; " + sanitized_domain_url.getHost() + ".pdf");

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType("application/pdf"))
					.cacheControl(CacheControl.noCache()).headers(headers)
					.body(new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray())));
		}
    }
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class PageNotFoundError extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 794045239226319408L;

	public PageNotFoundError() {
		super("Oh no! We couldn't find the page you want to audit.");
	}
}

