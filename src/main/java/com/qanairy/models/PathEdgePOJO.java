package com.qanairy.models;

import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.edges.PathEdge;

public class PathEdgePOJO extends PathEdge {

	private String key;
	private int transition_index;
	private PathObject path_obj_out;
	private PathObject path_obj_in;

	@Override
	public String getPathKey() {
		return this.key;
	}

	@Override
	public void setPathKey(String pathKey) {
		this.key = pathKey;
	}

	@Override
	public Integer getTransitionIndex() {
		return this.transition_index;
	}

	@Override
	public void setTransitionIndex(int idx) {
		this.transition_index = idx;
	}

	@Override
	public PathObject getPathObjectOut() {
		return path_obj_out;
	}

	@Override
	public PathObject getPathObjectIn() {
		return path_obj_in;
	}
	
}
