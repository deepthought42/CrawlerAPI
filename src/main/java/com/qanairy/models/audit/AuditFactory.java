package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Executes various {@link Audit audits}
 */
public class AuditFactory {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditFactory.class);

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
	public static List<Audit> executePageAudit(AuditCategory category, PageState page, String user_id) throws MalformedURLException, URISyntaxException {
		assert category != null;
		assert page != null;
		
		List<Audit> audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			LinksAudit link_audit = new LinksAudit();
			double link_score = link_audit.execute(page, user_id);
			audits.add(link_audit);
		}
		else if(AuditCategory.COLOR_MANAGEMENT.equals(category)) {
			ColorPaletteAudit color_palette_audit = new ColorPaletteAudit();
			double palette_score = color_palette_audit.execute(page, user_id);
			
			TextColorContrastAudit text_contrast_audit = new TextColorContrastAudit();
			double text_contrast_score = text_contrast_audit.execute(page, user_id);
					
			audits.add(color_palette_audit);
			audits.add(text_contrast_audit);
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
	public static List<Audit> executeDomainAudit(AuditCategory category, List<AuditRecord> audit_records, String user_id) throws MalformedURLException, URISyntaxException {
		assert category != null;
		assert audit_records != null;
		
		List<Audit> domain_audits = new ArrayList<Audit>();
		if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			
		}
		else if(AuditCategory.COLOR_MANAGEMENT.equals(category)) {
			log.warn("runing color manageent domain audit...");
			//TODO collect color pallete audits from audit records
			List<Audit> color_palette_audits = new ArrayList<>();
			for(AuditRecord record : audit_records) {
				for(Audit audit : record.getAudits()) {		
					if(audit.getSubcategory().equals(AuditSubcategory.COLOR_PALETTE)) {
						color_palette_audits.add(audit);
					}
					if(audit.getSubcategory().equals(AuditSubcategory.TEXT_BACKGROUND_CONTRAST)) {
						//TODO
					}
				}
			}
			DomainColorPaletteAudit color_palette_audit = new DomainColorPaletteAudit();
			double palette_score = color_palette_audit.execute(color_palette_audits);
					
			domain_audits.add(color_palette_audit);
		}
		
		return domain_audits;
	}
}
