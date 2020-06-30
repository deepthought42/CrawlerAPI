package com.qanairy.models.audit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

import com.qanairy.models.PageState;
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
	public DomainAudit(AuditCategory category, String description, AuditSubcategory subcategory) {
		setDescription(description);
		setSubcategory(subcategory);
		setCategory(category);
		setCreatedAt(LocalDateTime.now());
		setKey(generateKey());
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

	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public String generateKey() {
		return "audit::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getSubcategory().toString()+this.getCategory()+this.getCreatedAt().toString()+this.getScore());
	}
	
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
