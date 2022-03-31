package com.looksee.models.message;

import java.net.URL;

import com.looksee.models.enums.BrowserType;

import akka.actor.ActorRef;

public class UrlMessage extends Message{
	private URL url;
	private ActorRef discovery_actor;
	private ActorRef domain_actor;
	private BrowserType browser;
	
	public UrlMessage(ActorRef discovery_actor, 
					  URL url, 
					  BrowserType browser, 
					  ActorRef domain_actor, 
					  long domain_id, 
					  long account_id){
		setDiscoveryActor(discovery_actor);
		setUrl(url);
		setBrowser(browser);
		setDomainActor(domain_actor);
		setDomainId(domain_id);
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
}
