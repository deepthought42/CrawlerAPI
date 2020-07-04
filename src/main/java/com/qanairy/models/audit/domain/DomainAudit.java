package com.qanairy.models.audit.domain;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditSubcategory;

/**
 * Defines the globally required fields for all audits
 */
public abstract class DomainAudit extends Audit{
	private String description; //definition
	
	/**
	 * Construct empty action object
	 */
	public DomainAudit(){
		setCreatedAt(LocalDateTime.now());
	}
	
	/**
	 * 
	 * @param category
	 * @param best_practices
	 * @param ada_compliance_description
	 * @param description
	 * @param name
	 */
	public DomainAudit(AuditCategory category, List<String> best_practices, String ada_compliance, String description, AuditSubcategory subcategory) {
		super(category, best_practices, ada_compliance, description, subcategory);
	}

	/**
	 * Executes the audit using the given page state and user
	 * 
	 * @param page_state {@link PageState page} to be audited
	 * @param user_id 
	 * 
	 * @return score calculated for audit on page
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	public abstract double execute(List<Audit> audits);
	
	public abstract DomainAudit clone();
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.getKey();
	}
}
