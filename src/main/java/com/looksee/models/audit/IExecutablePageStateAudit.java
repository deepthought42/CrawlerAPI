package com.looksee.models.audit;

import com.looksee.models.PageState;

public interface IExecutablePageStateAudit {
	/**
	 * Executes audit on {@link PageState page}
	 * 
	 * @param page_state
	 * @param audit_record TODO
	 * @return
	 */
	public Audit execute(PageState page_state, AuditRecord audit_record);
}
