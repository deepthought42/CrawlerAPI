package com.qanairy.workManagement;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages work allowance statuses for accounts
 *
 */
public class WorkAllowanceStatus {
	private static Map<String, Boolean> accountStatuses = new HashMap<String, Boolean>();

	/**
	 * Accounts for presence of actor thread running
	 * 
	 * @param key Account key
	 * @return whether or not the status was successfully changed
	 */
	public static Boolean register(String key){
		return accountStatuses.put(key, true);
	}
	
	/**
	 * Accounts for presence of actor thread shutting down
	 * 
	 * @param key Account key
	 * @return whether or not the status was successfully changed
	 */
	public static Boolean haltWork(String key){
		return accountStatuses.put(key, false);
	}
	
	/**
	 * Checks status of work for a given account. If there is no entry, then it is assumed work 
	 * should not be allowed for the given account
	 * 
	 * @return whether or not the given account should be allowed to perform work
	 */
	public static boolean checkStatus(String key){
		if(accountStatuses.containsKey(key)){
			return accountStatuses.get(key);
		}
		return false;
	}
}
