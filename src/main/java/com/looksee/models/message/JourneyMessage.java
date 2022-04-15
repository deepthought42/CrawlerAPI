package com.looksee.models.message;

import java.util.ArrayList;
import java.util.List;

import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.PathStatus;
import com.looksee.models.journeys.Step;

public class JourneyMessage extends Message {

	private List<Step> steps;
	private PathStatus status;
	private BrowserType browser;
	
	public JourneyMessage(List<Step> steps,
					   PathStatus status, 
					   BrowserType browser, 
					   long domain_id, 
					   long account_id){
		setSteps(steps);
		setStatus(status);
		setBrowser(browser);
		setDomainId(domain_id);
		setAccountId(account_id);
	}
	
	public JourneyMessage clone(){
		return new JourneyMessage(new ArrayList<>(getSteps()), 
								  getStatus(), 
								  getBrowser(), 
								  getDomainId(), 
								  getAccountId());
	}

	public PathStatus getStatus() {
		return status;
	}

	private void setStatus(PathStatus status) {
		this.status = status;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	public void setBrowser(BrowserType browser) {
		this.browser = browser;
	}

	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}
}
