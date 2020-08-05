package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Domain;
import com.qanairy.models.PageState;
import com.qanairy.models.audit.domain.DomainColorPaletteAudit;
import com.qanairy.models.audit.domain.DomainFontAudit;
import com.qanairy.models.audit.domain.DomainLinksAudit;
import com.qanairy.models.audit.domain.DomainMarginAudit;
import com.qanairy.models.audit.domain.DomainNonTextColorContrastAudit;
import com.qanairy.models.audit.domain.DomainPaddingAudit;
import com.qanairy.models.audit.domain.DomainTextColorContrastAudit;
import com.qanairy.models.enums.AuditCategory;

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
	private ColorPaletteAudit color_palette_auditor;
	
	@Autowired
	private TextColorContrastAudit text_contrast_auditor;
	
	@Autowired
	private NonTextColorContrastAudit non_text_contrast_auditor;
	
	@Autowired
	private DomainFontAudit domain_font_auditor;

	@Autowired
	private FontAudit font_auditor;
	
	@Autowired
	private DomainPaddingAudit domain_padding_auditor;
	
	@Autowired
	private DomainMarginAudit domain_margin_auditor;
	
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
	 * Executes all pre-render audits for the {@link AuditCategory category} provided
	 * 
	 * @param category {@link AuditCategory} that we want to run audits for
	 * @param page {@link PageState page} that audit should be executed against
	 * @return {@linkplain List} of {@link Audit audits} executed 
	 * @throws URISyntaxException 
	 * @throws MalformedURLException 
	 * 
	 * @pre category != null
	 * @pre page != null
	 */
	public List<Audit> executePrerenderPageAudits(AuditCategory category, PageState page) throws MalformedURLException, URISyntaxException {
		assert category != null;
		assert page != null;
		
		List<Audit> audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			Audit link_audit = links_auditor.execute(page);
			audits.add(link_audit);
		}
		else if(AuditCategory.COLOR_MANAGEMENT.equals(category)) {
			/*works but temp disabled
			Audit color_palette_audit = color_palette_auditor.execute(page);
			Audit text_contrast_audit = text_contrast_auditor.execute(page);
			Audit non_text_contrast_audit = non_text_contrast_auditor.execute(page);
	
			audits.add(color_palette_audit);
			audits.add(text_contrast_audit);
			audits.add(non_text_contrast_audit);
			*/
		}
		else if(AuditCategory.TYPOGRAPHY.equals(category)) {
			/*
			Audit typeface_audit = typeface_auditor.execute(page);
			Audit font_audit = font_auditor.execute(page);
			
			audits.add(typeface_audit);
			audits.add(font_audit);
			*/
		}		
		
		return audits;
	}
	
	/**
	 * Executes all pre-render audits for the {@link AuditCategory category} provided
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
	public List<Audit> executePostRenderPageAudits(AuditCategory category, PageState page) throws MalformedURLException, URISyntaxException {
		assert category != null;
		assert page != null;
		
		List<Audit> audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			
		}
		else if(AuditCategory.COLOR_MANAGEMENT.equals(category)) {
			/*works but temp disabled
			Audit color_palette_audit = color_palette_auditor.execute(page);
			Audit text_contrast_audit = text_contrast_auditor.execute(page);
			Audit non_text_contrast_audit = non_text_contrast_auditor.execute(page);
	
			audits.add(color_palette_audit);
			audits.add(text_contrast_audit);
			audits.add(non_text_contrast_audit);
			*/
		}
		else if(AuditCategory.TYPOGRAPHY.equals(category)) {
			
			//Audit typeface_audit = typeface_auditor.execute(page);
			//audits.add(typeface_audit);

			//Audit font_audit = font_auditor.execute(page);
			//audits.add(font_audit);
			
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
	public List<Audit> executePrerenderDomainAudit(AuditCategory category, Domain domain) throws MalformedURLException, URISyntaxException {
		assert category != null;
		
		List<Audit> domain_audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {			
			Audit link_audit = domain_links_auditor.execute(domain);
			
			domain_audits.add(link_audit);
		}
		else if(AuditCategory.COLOR_MANAGEMENT.equals(category)) {
			log.warn("runing color manageent domain audit...");

			Audit color_palette_audit = domain_color_palette_auditor.execute(domain);
			
			domain_audits.add(color_palette_audit);
		}
		else if(AuditCategory.TYPOGRAPHY.equals(category)) {
			
		}
		return domain_audits;
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
	public List<Audit> executePostRenderDomainAudit(AuditCategory category, Domain domain) throws MalformedURLException, URISyntaxException {
		assert category != null;
		
		List<Audit> domain_audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			Audit padding_audits = domain_padding_auditor.execute(domain);
			domain_audits.add(padding_audits);

			Audit margin_audits = domain_margin_auditor.execute(domain);
			domain_audits.add(margin_audits);
		}
		else if(AuditCategory.COLOR_MANAGEMENT.equals(category)) {
			log.warn("running color manageent domain audit...");

			Audit color_palette_audit = domain_color_palette_auditor.execute(domain);
			Audit text_contrast_audit = domain_text_contrast_auditor.execute(domain);
			Audit non_text_contrast_audit = domain_non_text_contrast_auditor.execute(domain);
			
			domain_audits.add(color_palette_audit);
			domain_audits.add(text_contrast_audit);
			domain_audits.add(non_text_contrast_audit);
		}
		else if(AuditCategory.TYPOGRAPHY.equals(category)) {
			
			//Audit typeface_audit = typeface_auditor.execute(page);
			//audits.add(typeface_audit);
			
			Audit font_audit = domain_font_auditor.execute(domain);
			domain_audits.add(font_audit);
			
		}
		return domain_audits;
	}
}
