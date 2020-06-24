package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;

/**
 * Executes various {@link Audit audits}
 */
public class AuditFactory {

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
	public static List<Audit> execute(AuditCategory category, PageState page, String user_id) throws MalformedURLException, URISyntaxException {
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
			audits.add(color_palette_audit);
		}
		
		return audits;
	}
}
