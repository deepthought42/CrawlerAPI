package com.qanairy.models;

/**
 * Defines the settings of a service package (eg. how many users, how many domains/projects, etc..)
 */
public class ServicePackage {

	private String key;
	private String name;
	private int price;
	private int max_users;
	
	public ServicePackage(){
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ServicePackage(String name, int price, int max_users) {
		this.setName(name);
		this.setPrice(price);
		this.setMaxUsers(max_users);
		this.setKey(null);
	}

	/**
	 * {@inheritDoc}
	 */
	public ServicePackage(String key, String name, int price, int max_users) {
		this.setName(name);
		this.setPrice(price);
		this.setMaxUsers(max_users);
		this.setKey(key);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		if(o.getClass().equals(ServicePackage.class)){
			return true;
		}
		return false;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the price
	 */
	public int getPrice() {
		return price;
	}

	/**
	 * @param price the price to set
	 */
	public void setPrice(int price) {
		this.price = price;
	}

	/**
	 * @return the max_users
	 */
	public int getMaxUsers() {
		return max_users;
	}

	/**
	 * @param max_users the max_users to set
	 */
	public void setMaxUsers(int max_users) {
		this.max_users = max_users;
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}
}
