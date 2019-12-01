package com.qanairy.models.message;

import com.qanairy.models.Form;
import com.qanairy.models.PageState;

import akka.actor.ActorRef;

/**
 * Message that transports {@link Form} and {@link PageState}
 */
public class FormMessage {

	private Form form;
	private PageState page;
	private ActorRef domain_actor;
	private ActorRef discovery_actor;
	private String account_key;
	
	public FormMessage(Form form, PageState page, String account_key) {
		setForm(form);
		setPage(page);
		setAccountKey(account_key);
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

	public PageState getPage() {
		return page;
	}

	public void setPage(PageState page) {
		this.page = page;
	}

	public String getAccountKey() {
		return account_key;
	}

	public void setAccountKey(String account_key) {
		this.account_key = account_key;
	}
}
