package browsing;

import java.util.HashMap;

import util.ArrayUtility;

/**
 * A pairing of a name and a set of string values
 * 
 * @author Brandon Kindred
 *
 */
public class Attribute {
	public String name;
	public String[] vals;
	
	public Attribute(String attrName, String[] val){
		this.name = attrName;
		this.vals = val;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String[] getVal(){
		return this.vals;
	}
	
	public boolean equals(Attribute attr){
		if(this.getName().equals(attr.getName())
			&& ArrayUtility.joinArray(this.getVal()).equals(ArrayUtility.joinArray(attr.getVal()))){
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
		for(String val : this.getVal()){
			attrString += val;
			if(idx != this.getVal().length-1){
				attrString += ",";
			}
			idx++;
		}
		attrString += "]";
		return attrString;
	}
	
	@Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 5 + name.hashCode();
        
        for(String value : vals){
        	hash = hash * 13 + value.hashCode();
        }
        return hash;
    }
}
