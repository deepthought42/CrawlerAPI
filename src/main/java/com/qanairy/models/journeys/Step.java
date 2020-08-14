package com.qanairy.models.journeys;

import com.minion.browsing.Browser;
import com.qanairy.models.LookseeObject;

/**
 * A set of Steps
 */
public abstract class Step extends LookseeObject {

	public abstract void execute(Browser browser);
	
	public Step() {
		super();
	}
}
