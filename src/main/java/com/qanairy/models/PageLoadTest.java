package com.qanairy.models;

public class PageLoadTest extends Test{

	private PageState page_state;

	
	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	public String generateKey() {
		String path_key =  String.join("::", getPathKeys());
		path_key += getResult().getKey();
		
		return "test::"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(path_key);
	}
}
