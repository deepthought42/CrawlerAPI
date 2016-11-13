package com.minion.browsing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.persistence.DataAccessObject;
import com.minion.persistence.IAttribute;
import com.minion.persistence.IPersistable;
import com.minion.persistence.OrientConnectionFactory;
import com.minion.util.ArrayUtility;

/**
 * A pairing of a name and a set of string values
 * 
 * @author Brandon Kindred
 *
 */
public class Attribute implements IPersistable<IAttribute> {
    private static final Logger log = LoggerFactory.getLogger(Page.class);

	public String key;
	public String name;
	public String[] vals;
	
	public Attribute(String attrName, String[] val){
		this.name = attrName;
		this.vals = val;
		this.key = generateKey();
	}
	
	public boolean equals(Attribute attr){
		if(this.getName().equals(attr.getName())
			&& ArrayUtility.joinArray(this.getVals()).equals(ArrayUtility.joinArray(attr.getVals()))){
			return true;
		}
		return false;
	}
	
	public HashMap<String, String> toHash(){
		HashMap<String, String> hash = new HashMap<String, String>();
		hash.put("name", this.name);
		hash.put("values", this.vals.toString());
		return hash;
	}
	
	public String toString(){
		String attrString = "{";
		
		attrString += "name : " + this.getName() + ", ";
		attrString += "values : [";
		int idx = 0;
		for(String val : this.getVals()){
			attrString += val;
			if(idx != this.getVals().length-1){
				attrString += ",";
			}
			idx++;
		}
		attrString += "]";
		return attrString;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 5 + name.hashCode();
        
        for(String value : vals){
        	hash = hash * 13 + value.hashCode();
        }
        return hash;
    }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return this.name.hashCode()+":";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAttribute convertToRecord(OrientConnectionFactory connection) {
		Iterator<IAttribute> attributes = (Iterator<IAttribute>) DataAccessObject.findByKey(this.getKey(), connection, IAttribute.class).iterator();
		IAttribute attribute = null;
		if(!attributes.hasNext()){
			attribute = connection.getTransaction().addVertex("class:"+Attribute.class.getCanonicalName()+","+UUID.randomUUID(), IAttribute.class);
			attribute.setName(this.name);
			attribute.setVals(this.vals);
			attribute.setKey(this.key);
		}
		else{
			attribute = attributes.next();
		}
		
		return attribute;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IAttribute> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IAttribute> update() {
		Iterator<IAttribute> attribute_iter = (Iterator<IAttribute>) DataAccessObject.findByKey(this.generateKey(), IAttribute.class).iterator();
		int cnt=0;
		while(attribute_iter.hasNext()){
			attribute_iter.next();
			cnt++;
		}
		log.info("# of existing Attribute records with key "+this.getKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IAttribute attribute = null;
		if(cnt == 0){
			attribute = connection.getTransaction().addVertex("class:"+Attribute.class.getCanonicalName()+","+UUID.randomUUID(), IAttribute.class);	
		}
		
		this.convertToRecord(connection);
		connection.save();
		
		return this;
	}
	

	public String getKey() {
		return this.hashCode()+"";
	}

	public String getName(){
		return this.name;
	}
	
	public String[] getVals(){
		return this.vals;
	}
}
