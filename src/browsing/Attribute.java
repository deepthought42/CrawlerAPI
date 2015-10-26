package browsing;

import util.ArrayUtility;

/**
 * A pairing of a name and a set of values
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
	
	public String toString(){
		String attrString = "";
		
		attrString += "NAME : '" + this.getName() + "', ";
		attrString += "VALUE : '" + this.getVal() + "'";
		
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
