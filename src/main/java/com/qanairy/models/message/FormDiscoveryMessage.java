package com.qanairy.models.message;

import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.enums.BrowserType;

import akka.actor.ActorRef;

public class FormDiscoveryMessage {

	private Form form;
	private BrowserType browser;
	private ActorRef domain_actor;
	private ActorRef discovery_actor; 
	private Domain domain;
	
	public FormDiscoveryMessage(Form form, BrowserType browser, Domain domain) {
		setForm(form);
		setBrowserType(browser);
	}

	public Form getForm() {
		return form;
	}

	private void setForm(Form form) {
		this.form = form;
	}

	public BrowserType getBrowserType() {
		return browser;
	}

	private void setBrowserType(BrowserType browser_type) {
		this.browser = browser_type;
	}

	public void setDomainActor(ActorRef domain_actor) {
		this.domain_actor = domain_actor;
	}

	public ActorRef getDomainActor() {
		return domain_actor;
	}

	public ActorRef getDiscoveryActor() {
		return discovery_actor;
	}

	public void setDiscoveryActor(ActorRef discovery_actor) {
		this.discovery_actor = discovery_actor;
	}

	public Domain getDomain() {
		return domain;
	}

	private void setDomain(Domain domain) {
		this.domain = domain;
	}
}
