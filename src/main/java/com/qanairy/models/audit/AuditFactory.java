package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qanairy.models.Domain;
import com.qanairy.models.PageState;
import com.qanairy.models.PageVersion;
import com.qanairy.models.audit.domain.DomainColorPaletteAudit;
import com.qanairy.models.audit.domain.DomainFontAudit;
import com.qanairy.models.audit.domain.DomainImageAltTextAudit;
import com.qanairy.models.audit.domain.DomainMarginAudit;
import com.qanairy.models.audit.domain.DomainNonTextColorContrastAudit;
import com.qanairy.models.audit.domain.DomainPaddingAudit;
import com.qanairy.models.audit.domain.DomainTextColorContrastAudit;
import com.qanairy.models.audit.domain.DomainTitleAndHeaderAudit;
import com.qanairy.models.audit.domain.DomainTypefaceAudit;
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
	private TitleAndHeaderAudit title_and_header_auditor;
	
	@Autowired
	private DomainFontAudit domain_font_auditor;

	@Autowired
	private ImageAltTextAudit image_alt_text_auditor;
	
	@Autowired
	private ParagraphingAudit paragraph_auditor;
	
	@Autowired
	private FontAudit font_auditor;

	@Autowired
	private TypefacesAudit typeface_auditor;
	
	@Autowired
	private PaddingAudit padding_auditor;
	
	@Autowired
	private MarginAudit margin_auditor;
	
	@Autowired
	private DomainPaddingAudit domain_padding_auditor;
	
	@Autowired
	private DomainMarginAudit domain_margin_auditor;
	

	@Autowired
	private DomainColorPaletteAudit domain_color_palette_auditor;
	
	@Autowired
	private DomainTextColorContrastAudit domain_text_contrast_auditor;
	
	@Autowired
	private DomainNonTextColorContrastAudit domain_non_text_contrast_auditor;
	
	@Autowired
	private DomainTitleAndHeaderAudit domain_title_and_header_auditor;
	
	@Autowired
	private DomainTypefaceAudit domain_typeface_auditor;
	
	@Autowired
	private DomainImageAltTextAudit domain_image_alt_text_auditor;
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
	public List<Audit> executePrerenderPageAudits(AuditCategory category, PageVersion page) throws MalformedURLException, URISyntaxException {
		assert category != null;
		assert page != null;
		
		List<Audit> audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			
		}
		else if(AuditCategory.AESTHETICS.equals(category)) {
			/*works but temp disabled
			Audit color_palette_audit = color_palette_auditor.execute(page);
			Audit text_contrast_audit = text_contrast_auditor.execute(page);
			Audit non_text_contrast_audit = non_text_contrast_auditor.execute(page);
	
			audits.add(color_palette_audit);
			audits.add(text_contrast_audit);
			audits.add(non_text_contrast_audit);
			 */
			
		}
		else if(AuditCategory.CONTENT.equals(category)) {
		
		}		
		
		return audits;
	}
	
	/**
	 * Executes all post-render audits for the {@link AuditCategory category} provided
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
	public List<Audit> executePostRenderPageAudits(
							AuditCategory category, 
							PageState page
	) throws MalformedURLException, URISyntaxException {
		assert category != null;
		assert page != null;
		
		List<Audit> audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			
			Audit link_audit = links_auditor.execute(page);
			audits.add(link_audit);
			
			Audit title_and_headers = title_and_header_auditor.execute(page);
			audits.add(title_and_headers);
			
		}
		else if(AuditCategory.AESTHETICS.equals(category)) {
			//works but temp disabled
		
			Audit color_palette_audit = color_palette_auditor.execute(page);
			audits.add(color_palette_audit);

			Audit text_contrast_audit = text_contrast_auditor.execute(page);
			audits.add(text_contrast_audit);
			/*
			Audit padding_audits = padding_auditor.execute(page);
			audits.add(padding_audits);

			Audit margin_audits = margin_auditor.execute(page);
			audits.add(margin_audits);
			 */
			Audit non_text_contrast_audit = non_text_contrast_auditor.execute(page);
			audits.add(non_text_contrast_audit);
		}
		else if(AuditCategory.CONTENT.equals(category)) {
			/* NOTE typeface audit is incomplete and currently commented out
			 
			Audit typeface_audit = typeface_auditor.execute(page);
			audits.add(typeface_audit);
			 */
			
			Audit alt_text_audit = image_alt_text_auditor.execute(page);
			audits.add(alt_text_audit);
			
			//Audit font_audit = font_auditor.execute(page);
			//audits.add(font_audit);
			
			Audit paragraph_audit = paragraph_auditor.execute(page);
			audits.add(paragraph_audit);	
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
		assert domain != null;
		assert category != null;
		
		List<Audit> domain_audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {			
			
		}
		else if(AuditCategory.AESTHETICS.equals(category)) {
			
		}
		else if(AuditCategory.CONTENT.equals(category)) {
			
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
	 * @throws JsonProcessingException 
	 * 
	 * @pre category != null
	 * @pre page != null
	 */
	public List<Audit> executePostRenderDomainAudit(AuditCategory category, Domain domain) throws MalformedURLException, URISyntaxException, JsonProcessingException {
		assert category != null;
		
		List<Audit> domain_audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			
			/*
			MessageBroadcaster.broadcastAuditMessage(domain.getHost(), new AuditMessage(category, AuditSubcategory.LINKS, "Starting link audit"));
			Audit link_audit = domain_links_auditor.execute(domain);
			domain_audits.add(link_audit);
			
			MessageBroadcaster.broadcastAuditMessage(domain.getHost(), new AuditMessage(category, AuditSubcategory.TITLES, "Starting title and header audit"));
			Audit title_and_headers = domain_title_and_header_auditor.execute(domain);
			domain_audits.add(title_and_headers);
			
			MessageBroadcaster.broadcastAuditMessage(domain.getHost(), new AuditMessage(category, AuditSubcategory.PADDING, "Starting padding audit"));
			Audit padding_audits = domain_padding_auditor.execute(domain);
			domain_audits.add(padding_audits);

			MessageBroadcaster.broadcastAuditMessage(domain.getHost(), new AuditMessage(category, AuditSubcategory.MARGIN, "Starting margin audit"));
			Audit margin_audits = domain_margin_auditor.execute(domain);
			domain_audits.add(margin_audits);
			*/
		}
		else if(AuditCategory.AESTHETICS.equals(category)) {
			log.warn("running color manageent domain audit...");
			/*
			Audit color_palette_audit = domain_color_palette_auditor.execute(domain);			
			Audit text_contrast_audit = domain_text_contrast_auditor.execute(domain);
			Audit non_text_contrast_audit = domain_non_text_contrast_auditor.execute(domain);
			
			domain_audits.add(color_palette_audit);
			domain_audits.add(text_contrast_audit);
			domain_audits.add(non_text_contrast_audit);
			*/
		}
		else if(AuditCategory.CONTENT.equals(category)) {
			
			//Audit domain_typeface_audit = domain_typeface_auditor.execute(domain);
			//domain_audits.add(domain_typeface_audit);
			
			//Audit font_audit = domain_font_auditor.execute(domain);
			//domain_audits.add(font_audit);
			
			//Audit alt_text_audit = domain_image_alt_text_auditor.execute(domain);
			//domain_audits.add(alt_text_audit);			
		}
	
		return domain_audits;
	}
}
