package com.qanairy.models;

import java.util.List;

import org.apache.commons.collections.ListUtils;

import com.qanairy.persistence.Attribute;

/**
 * A pairing of a name and a set of string values
 */
public class AttributePOJO extends Attribute {

	public String key;
	public String name;
	public List<String> vals;
	
	/**
	 * 
	 * @param attrName
	 * @param val
	 * 
	 * @pre attrName != null
	 * @pre val != null;
	 */
	public AttributePOJO(String attrName, List<String> vals){
		assert attrName != null;
		assert vals != null;
		
		this.name = attrName;
		this.vals = vals;
		setKey(generateKey());
	}
	
	public AttributePOJO(String key, String attrName, List<String> vals){
		this.name = attrName;
		this.vals = vals;
		this.setKey(generateKey());
	}
	
	/**
	 * Checks if a value is assigned to this Attribute
	 * 
	 * @return true if value exists, otherwise false
	 */
	public boolean contains(String val){
		for(String value : this.vals){
			if(value.trim().equals(val)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj){
		if(obj instanceof Attribute){
			Attribute attr = (Attribute)obj;
			
			if(this.getName().equals(attr.getName())
				&& ListUtils.subtract(this.getVals(), attr.getVals()).size() == 0){
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
			if(idx != this.getVals().size()-1){
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

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public List<String> getVals(){
		return this.vals;
	}
	
	public void setVals(List<String> val_list){
		this.vals = val_list;
	}
	
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(getVals().toString());
	}
}
