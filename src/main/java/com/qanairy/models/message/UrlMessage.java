package com.qanairy.models.message;

import java.net.URL;

import com.qanairy.services.BrowserType;

import akka.actor.ActorRef;

public class UrlMessage {
	private URL url;
	private ActorRef discovery_actor;
	private BrowserType browser;
	
	public UrlMessage(ActorRef discovery_actor, URL url, BrowserType browser){
		setDiscoveryActor(discovery_actor);
		setUrl(url);
		setBrowser(browser);
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
}
