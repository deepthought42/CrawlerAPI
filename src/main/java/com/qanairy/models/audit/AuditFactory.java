package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.PageState;
import com.qanairy.models.audit.domain.DomainColorPaletteAudit;
import com.qanairy.models.audit.domain.DomainLinksAudit;
import com.qanairy.models.audit.domain.DomainNonTextColorContrastAudit;
import com.qanairy.models.audit.domain.DomainTextColorContrastAudit;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Executes various {@link Audit audits}
 */
@Component
public class AuditFactory {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditFactory.class);
	
	@Autowired
	private LinksAudit links_auditor;
	
	@Autowired
	private ColorPaletteAuditor color_palette_auditor;
	
	@Autowired
	private TextColorContrastAudit text_contrast_auditor;
	
	@Autowired
	private NonTextColorContrastAudit non_text_contrast_auditor;
	
	@Autowired
	private FontAudit font_auditor;
	
	@Autowired
	private PaddingAudit padding_auditor;
	
	@Autowired
	private MarginAudit margin_auditor;
	

	//Domain audits
	@Autowired
	private DomainLinksAudit domain_links_auditor;

	@Autowired
	private DomainColorPaletteAudit domain_color_palette_auditor;
	
	@Autowired
	private DomainTextColorContrastAudit domain_text_contrast_auditor;
	
	@Autowired
	private DomainNonTextColorContrastAudit domain_non_text_contrast_auditor;
	
	@Autowired
	private TypefacesAudit typeface_auditor;
	
	/**
	 * Executes all audits for the {@link AuditCategory category} provided
	 * 
	 * @param category {@link AuditCategory} that we want to run audits for
	 * @param page {@link PageState page} that audit should be executed against
	 * 
	 * @return {@linkplain List} of {@link Audit audits} executed 
	 * @throws URISyntaxException 
	 * @throws MalformedURLException 
	 * 
	 * @pre category != null
	 * @pre page != null
	 */
	public List<Audit> executePageAudit(AuditCategory category, PageState page, String user_id) throws MalformedURLException, URISyntaxException {
		assert category != null;
		assert page != null;
		
		List<Audit> audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			Audit link_audit = links_auditor.execute(page);
			Audit padding_audits = padding_auditor.execute(page);
			Audit margin_audits = margin_auditor.execute(page);
			
			audits.add(link_audit);
			audits.add(padding_audits);
			audits.add(margin_audits);
		}
		else if(AuditCategory.COLOR_MANAGEMENT.equals(category)) {
			Audit color_palette_audit = color_palette_auditor.execute(page);
			Audit text_contrast_audit = text_contrast_auditor.execute(page);
			Audit non_text_contrast_audit = non_text_contrast_auditor.execute(page);
			
			audits.add(color_palette_audit);
			audits.add(text_contrast_audit);
			audits.add(non_text_contrast_audit);
		}
		else if(AuditCategory.TYPOGRAPHY.equals(category)) {
			Audit typeface_audit = typeface_auditor.execute(page);
			Audit font_audit = font_auditor.execute(page);
			
			audits.add(typeface_audit);
			audits.add(font_audit);
		}		
		
		return audits;
	}
	
	/**
	 * Executes all audits for the {@link AuditCategory category} provided
	 * 
	 * @param category {@link AuditCategory} that we want to run audits for
	 * @param page {@link PageState page} that audit should be executed against
	 * 
	 * @return {@linkplain List} of {@link Audit audits} executed 
	 * @throws URISyntaxException 
	 * @throws MalformedURLException 
	 * 
	 * @pre category != null
	 * @pre page != null
	 */
	public List<Audit> executeDomainAudit(AuditCategory category, List<Audit> audits) throws MalformedURLException, URISyntaxException {
		assert category != null;
		assert audits != null;
		
		List<Audit> domain_audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			List<Audit> link_audits = new ArrayList<>();

			for(Audit audit : audits) {		
				if(audit.getSubcategory().equals(AuditSubcategory.LINKS)) {
					link_audits.add(audit);
				}
			}
			
			Audit link_audit = domain_links_auditor.execute(link_audits);
			
			domain_audits.add(link_audit);
		}
		else if(AuditCategory.COLOR_MANAGEMENT.equals(category)) {
			log.warn("runing color manageent domain audit...");
			//TODO collect color pallete audits from audit records
			List<Audit> color_palette_audits = new ArrayList<>();
			List<Audit> text_contrast_audits = new ArrayList<>();
			List<Audit> non_text_contrast_audits = new ArrayList<>();
	
			for(Audit audit : audits) {		
				if(audit.getSubcategory().equals(AuditSubcategory.COLOR_PALETTE)) {
					color_palette_audits.add(audit);
				}
				else if(audit.getSubcategory().equals(AuditSubcategory.TEXT_BACKGROUND_CONTRAST)) {
					text_contrast_audits.add(audit);
				}
				else if(audit.getSubcategory().equals(AuditSubcategory.NON_TEXT_BACKGROUND_CONTRAST)) {
					non_text_contrast_audits.add(audit);
				}					
			}

			Audit color_palette_audit = domain_color_palette_auditor.execute(color_palette_audits);
			Audit text_contrast_audit = domain_text_contrast_auditor.execute(text_contrast_audits);
			Audit non_text_contrast_audit = domain_non_text_contrast_auditor.execute(non_text_contrast_audits);
			
			domain_audits.add(color_palette_audit);
			domain_audits.add(text_contrast_audit);
			domain_audits.add(non_text_contrast_audit);
		}

		return domain_audits;
	}
}
