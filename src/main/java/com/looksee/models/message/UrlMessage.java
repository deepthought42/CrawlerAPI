package com.looksee.models.message;

import java.net.URL;

import com.looksee.models.Domain;
import com.looksee.models.enums.BrowserType;

import akka.actor.ActorRef;

public class UrlMessage {
	private URL url;
	private ActorRef discovery_actor;
	private ActorRef domain_actor;
	private BrowserType browser;
	private Domain domain;
	private String account;
	
	public UrlMessage(ActorRef discovery_actor, URL url, BrowserType browser, ActorRef domain_actor, Domain domain, String account_id){
		setDiscoveryActor(discovery_actor);
		setUrl(url);
		setBrowser(browser);
		setDomainActor(domain_actor);
		setDomain(domain);
		setAccountId(account_id);
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public ActorRef getDiscoveryActor() {
		return discovery_actor;
	}

	public void setDiscoveryActor(ActorRef discovery_actor) {
		this.discovery_actor = discovery_actor;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	private void setBrowser(BrowserType browser) {
		this.browser = browser;
	}

	public ActorRef getDomainActor() {
		return domain_actor;
	}

	public void setDomainActor(ActorRef domain_actor) {
		this.domain_actor = domain_actor;
	}

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}

	public String getAccountId() {
		return account;
	}

	public void setAccountId(String account) {
		this.account = account;
	}
}
