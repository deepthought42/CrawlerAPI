package com.qanairy.models.message;

import com.qanairy.models.Domain;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserType;

import akka.actor.ActorRef;

public class TestMessage {

	private Test test;
	private Domain domain;
	private ActorRef discovery_actor;	
	private ActorRef domain_actor;
	private BrowserType browser;
	
	public TestMessage(Test test, ActorRef discovery_actor, BrowserType browser, ActorRef domain_actor, Domain domain){
		setTest(test);
		setDiscoveryActor(discovery_actor);
		setBrowser(browser);
		setDomainActor(domain_actor);
		setDomain(domain);
	}
	
	public TestMessage clone(){
		return new TestMessage(test, getDiscoveryActor(), getBrowser(), getDomainActor(), getDomain());
	}

	public ActorRef getDiscoveryActor() {
		return discovery_actor;
	}

	private void setDiscoveryActor(ActorRef discovery_actor) {
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

	public Test getTest() {
		return test;
	}

	public void setTest(Test test) {
		this.test = test;
	}

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}
}
