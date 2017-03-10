package com.qanairy.models;

import com.minion.util.ArrayUtility;

/**
 * A pairing of a name and a set of string values
 */
public class Attribute {

	public String key;
	public String name;
	public String[] vals;
	
	public Attribute(String attrName, String[] val){
		this.name = attrName;
		this.vals = val;
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
