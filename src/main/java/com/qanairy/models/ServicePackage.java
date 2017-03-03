package com.qanairy.models;

import java.util.Iterator;
import java.util.UUID;

import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAccount;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.IServicePackage;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines the settings of a service package (eg. how many users, how many domains/projects, etc..)
 */
public class ServicePackage implements IPersistable<IServicePackage>{

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
		this.name = name;
		this.price = price;
		this.max_users = max_users;
		this.key = generateKey();
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IServicePackage convertToRecord(OrientConnectionFactory connection) {
		this.setKey(this.generateKey());
		
		IServicePackage svc_pkg = connection.getTransaction().addVertex("class:"+IServicePackage.class.getCanonicalName()+","+UUID.randomUUID(), IServicePackage.class);
		svc_pkg.setKey(this.generateKey());
		svc_pkg.setName(this.name);
		svc_pkg.setPrice(this.price);
		svc_pkg.setMaxUsers(this.max_users);

		return svc_pkg;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IServicePackage create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		@SuppressWarnings("unchecked")
		Iterable<IServicePackage> svc_pkgs = (Iterable<IServicePackage>) DataAccessObject.findByKey(this.getKey(), orient_connection, IAccount.class);
		Iterator<IServicePackage> iter = svc_pkgs.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because account already exists
			return null;
		}
		IServicePackage svc_pkg = this.convertToRecord(orient_connection);
		orient_connection.save();
		return svc_pkg;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IServicePackage update() {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IServicePackage svc_pkg = this.convertToRecord(connection);		
		connection.save();
		
		return svc_pkg;
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
