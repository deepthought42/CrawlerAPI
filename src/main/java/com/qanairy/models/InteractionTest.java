package com.qanairy.models;

public class InteractionTest extends Test{

	private Path path;
	
	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	public String generateKey() {
		return "test::"+org.apache.commons.codec.digest.DigestUtils.sha512Hex(path.getKey());
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}
}
