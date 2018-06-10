package com.qanairy.models;

public class PathEdge {

	private String key;
	private int transition_index;
	private PathObject path_obj_out;
	private PathObject path_obj_in;

	public String getPathKey() {
		return this.key;
	}

	public void setPathKey(String pathKey) {
		this.key = pathKey;
	}

	public Integer getTransitionIndex() {
		return this.transition_index;
	}

	public void setTransitionIndex(int idx) {
		this.transition_index = idx;
	}

	public PathObject getPathObjectOut() {
		return path_obj_out;
	}

	public PathObject getPathObjectIn() {
		return path_obj_in;
	}
	
}
