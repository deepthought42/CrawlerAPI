package com.minion.browsing;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.util.ArrayUtility;
import com.qanairy.models.Page;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAttribute;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * A pairing of a name and a set of string values
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj){
		if(obj instanceof Attribute){
			Attribute attr = (Attribute)obj;
			if(this.getName().equals(attr.getName())
				&& ArrayUtility.joinArray(this.getVals()).equals(ArrayUtility.joinArray(attr.getVals()))){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
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
	public String generateKey() {
		return this.name.hashCode()+":";
	}

	/**
	 * {@inheritDoc}
	 */
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
	public IAttribute create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		IAttribute attribute = this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return attribute;
	}

	/**
	 * {@inheritDoc}
	 */
	public IAttribute update() {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IAttribute attribute = this.convertToRecord(connection);
		connection.save();
		
		return attribute;
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
