package com.qanairy.models.audit.domain;

import java.util.List;

import com.qanairy.models.Domain;
import com.qanairy.models.audit.Audit;

public interface IExecutableDomainAudit {
	/**
	 * Executes audit using list of {@link Audit audits}
	 * 
	 * @param audits {@link List} of audits to be analyzed as a whole
	 * 
 	 * @return {@link Audit audit}
	 */
	public Audit execute(Domain domain);
}
