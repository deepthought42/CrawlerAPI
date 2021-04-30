package com.looksee.models.journeys;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A set of Steps
 */
public class NavigationStep extends Step {
	private static Logger log = LoggerFactory.getLogger(NavigationStep.class);

	private String url;
	
	public NavigationStep() {}
	
	public NavigationStep(URL url) {
		assert url != null;

		setUrl(url.getProtocol()+"://"+url.getHost()+url.getPath());
		setKey(generateKey());
	}
	
	@Override
	public String generateKey() {
		return "navigationstep:"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(url.toString());
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
