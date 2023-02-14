package com.looksee.models;

import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;

public interface IExecutablePageStateAudit {
	/**
	 * Executes audit on {@link PageState page}
	 * 
	 * @param page_state
	 * @param audit_record TODO
	 * @param design_system TODO
	 * @return
	 */
	public Audit execute(PageState page_state, 
						 AuditRecord audit_record, 
						 DesignSystem design_system);
}
