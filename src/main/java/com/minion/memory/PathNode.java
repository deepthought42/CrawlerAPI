package com.minion.memory;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class PathNode {
	public final int hash_code;
	public final String classname;
	public final String string_rep;
	
	public PathNode(int id, String classname, String stringRep) {
		this.hash_code = id;
		this.classname = classname;
		this.string_rep = stringRep;
	}

}
