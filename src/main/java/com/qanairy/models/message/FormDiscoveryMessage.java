package com.qanairy.models.message;

import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;

import akka.actor.ActorRef;

/**
 * Message that transports {@link Form}, {@link DiscoveryRecord}, {@link PageState}, and {@link Domain} objects
 */
public class FormDiscoveryMessage {

	private Form form;
	private DiscoveryRecord discovery;
	private PageState page;
	private ActorRef domain_actor;
	private ActorRef discovery_actor; 
	private Domain domain;
	
	public FormDiscoveryMessage(Form form, DiscoveryRecord discovery, Domain domain, PageState page) {
		setForm(form);
		setDiscovery(discovery);
		setDomain(domain);
		setPage(page);
	}

	public Form getForm() {
		return form;
	}

	private void setForm(Form form) {
		this.form = form;
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

	public DiscoveryRecord getDiscovery() {
		return discovery;
	}

	public void setDiscovery(DiscoveryRecord discovery) {
		this.discovery = discovery;
	}

	public PageState getPage() {
		return page;
	}

	public void setPage(PageState page) {
		this.page = page;
	}
}
