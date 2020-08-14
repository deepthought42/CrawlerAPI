package com.qanairy.models.audit;

import com.qanairy.models.PageState;
import com.qanairy.models.Page;

public interface IExecutablePageVersionAudit {
	/**
	 * Executes audit on {@link PageState page}
	 * 
	 * @param page_state
	 * @return
	 */
	public Audit execute(Page page_version);
}
