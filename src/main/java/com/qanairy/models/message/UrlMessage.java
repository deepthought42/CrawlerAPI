package com.qanairy.models.message;

import java.net.URL;

import com.minion.actors.DiscoveryActor;
import com.qanairy.services.BrowserType;

import akka.actor.AbstractActor;

public class UrlMessage {
	private URL url;
	private AbstractActor discovery_actor;
	private BrowserType browser;
	
	public UrlMessage(DiscoveryActor discovery_actor, URL url, BrowserType browser){
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

	public AbstractActor getDiscoveryActor() {
		return discovery_actor;
	}

	public void setDiscoveryActor(DiscoveryActor discovery_actor) {
		this.discovery_actor = discovery_actor;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	private void setBrowser(BrowserType browser) {
		this.browser = browser;
	}
}
