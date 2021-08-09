package com.looksee.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.PageState;
import com.looksee.models.audit.aesthetics.ColorPaletteAudit;
import com.looksee.models.audit.aesthetics.FontAudit;
import com.looksee.models.audit.aesthetics.MarginAudit;
import com.looksee.models.audit.aesthetics.NonTextColorContrastAudit;
import com.looksee.models.audit.aesthetics.PaddingAudit;
import com.looksee.models.audit.aesthetics.TextColorContrastAudit;
import com.looksee.models.audit.aesthetics.TypefacesAudit;
import com.looksee.models.audit.content.ImageAltTextAudit;
import com.looksee.models.audit.content.ReadabilityAudit;
import com.looksee.models.audit.informationarchitecture.LinksAudit;
import com.looksee.models.audit.informationarchitecture.SecurityAudit;
import com.looksee.models.audit.informationarchitecture.TitleAndHeaderAudit;
import com.looksee.models.enums.AuditCategory;

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
	private ImageAltTextAudit image_alt_text_auditor;
	
	@Autowired
	private ParagraphingAudit paragraph_auditor;
	
	@Autowired
	private ReadabilityAudit readability_auditor;
	
	@Autowired
	private FontAudit font_auditor;

	@Autowired
	private SecurityAudit security_audit;
	
	@Autowired
	private TypefacesAudit typeface_auditor;
	
	@Autowired
	private PaddingAudit padding_auditor;
	
	@Autowired
	private MarginAudit margin_auditor;
	
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
	public List<Audit> executePageAudits(
							AuditCategory category, 
							PageState page
	) {
		assert category != null;
		assert page != null;
		
		List<Audit> audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			
			Audit link_audit = links_auditor.execute(page);
			audits.add(link_audit);
			
			Audit title_and_headers = title_and_header_auditor.execute(page);
			audits.add(title_and_headers);
			
			Audit security = security_audit.execute(page);
			audits.add(security);
			
			//Audit performance = 
		}
		else if(AuditCategory.AESTHETICS.equals(category)) {
			//works but temp disabled
		
			//Audit color_palette_audit = color_palette_auditor.execute(page);
			//audits.add(color_palette_audit);

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
			
			Audit readability_audit = readability_auditor.execute(page);
			audits.add(readability_audit);
			
			//Audit font_audit = font_auditor.execute(page);
			//audits.add(font_audit);
			
			Audit paragraph_audit = paragraph_auditor.execute(page);
			audits.add(paragraph_audit);	
		}

		
		return audits;
	}
}
