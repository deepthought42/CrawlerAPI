package com.minion.structs;

import java.util.HashMap;
import java.util.Map;

import com.qanairy.models.Test;
import com.qanairy.models.Action;
import com.qanairy.models.PageElement;


/**
 * Holds elements in a map, that is keyed by element action sequence and content. 
 * 
 * @author Brandon Kindred
 *
 */
public class TestMapper {
	private Map<String, Test> testHash;	
	
	/**
	 * Creates a new instance of the tracker
	 */
	public TestMapper(){
		testHash = new HashMap<String, Test>();
	}
	
	/**
	 * Adds a new entry to the element action path
	 * 
	 * @param elem
	 * @param action
	 */
	public void addTest(Test test){
		int hash_code = test.hashCode();
		
		if(!this.testHash.containsKey(Integer.toString(hash_code))){
			this.testHash.put(Integer.toString(hash_code), test);
		}
	}
	
	/**
	 * Generates a key with the format xpath:::content_hash:::action_name where
	 * the content_hash is a hash of the text within the element.
	 * 
	 * @param elem
	 * @param action
	 * @return
	 */
	private String generateHash(PageElement elem, Action action){
		int content_hash = elem.hashCode();
		String xpath = elem.getXpath();
		String action_str = action.getName();
		
		return xpath+":::"+content_hash+":::"+action_str;
	
	}
	
	/**
	 * Checks if element action sequence exists.
	 * 
	 * @param elem
	 * @param action
	 * @return
	 */
	public boolean containsTest(Test test){
		String hash_key = Integer.toString(test.hashCode());//generateHash(elem, action);
		return  this.testHash.containsKey(hash_key);
	}
	
	/** 
	 * @return hash of element action sequences
	 */
	public Map<String, Test> getTestHash(){
		return testHash;
	}
}
