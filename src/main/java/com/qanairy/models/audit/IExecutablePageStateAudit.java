package com.qanairy.models.audit;

import com.qanairy.models.PageState;

public interface IExecutablePageStateAudit {
	/**
	 * Executes audit on {@link PageState page}
	 * 
	 * @param page_state
	 * @return
	 */
	public Audit execute(PageState page_state);
}
