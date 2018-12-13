package com.qanairy.models;

/**
 * A {@link PathObject} object that signifies a change in view to either a new tab or window based 
 *  within a path
 *
 */
public class ViewSwap implements PathObject{

	private String key;
	private String type;
	
	public ViewSwap(){
		key = "view_swap";
		type = "view_swap";
	}
	
	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public void setKey(String key) {
		//do nothing
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
		//do nothing
	}

	

}
