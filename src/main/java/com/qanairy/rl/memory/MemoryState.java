package com.qanairy.rl.memory;

/**
 * A state consists of an identifier and an image formatted to base64
 * 
 * @author Brandon Kindred
 */
public class MemoryState {
	/**
	 * Identifier is meant to identify the state
	 */
	public int identifier = 0;
	
	/**
	 * 
	 * @param objects
	 */
	public MemoryState(int identifier) {
		this.setIdentifier(identifier);
	}

	/**
	 * 
	 * @return
	 */
	public int getIdentifier() {
		return this.identifier;
	}

	/**
	 * 
	 * @param identifier
	 */
	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}
}
