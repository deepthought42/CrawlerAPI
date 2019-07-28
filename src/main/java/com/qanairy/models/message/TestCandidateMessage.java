package com.qanairy.models.message;

import java.util.List;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.enums.BrowserType;

import akka.actor.ActorRef;

/**
 * 
 * 
 */
public class TestCandidateMessage {

	private List<String> keys;
	private List<PathObject> path_objects;
	private ActorRef discovery_actor;
	private ActorRef domain_actor;
	private PageState result_page;
	private BrowserType browser;
	
	public TestCandidateMessage(List<String> keys, List<PathObject> path_objects, ActorRef discovery_actor, PageState result_page, BrowserType browser, ActorRef domain_actor){
		setDiscoveryActor(discovery_actor);
		setBrowser(browser);
		this.keys = keys;
		this.path_objects = path_objects;
		this.result_page = result_page;
		setDomainActor(domain_actor);
	}

	public List<String> getKeys() {
		return keys;
	}

	public List<PathObject> getPathObjects() {
		return path_objects;
	}

	public PageState getResultPage() {
		return result_page;
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

	public void setBrowser(BrowserType browser) {
		this.browser = browser;
	}

	public ActorRef getDomainActor() {
		return domain_actor;
	}

	public void setDomainActor(ActorRef domain_actor) {
		this.domain_actor = domain_actor;
	}
}
