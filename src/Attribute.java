
public class Attribute {
	private String name;
	private String val;
	
	public Attribute(String attrName, String val){
		if(val == null){
			val = "";
		}
		this.name = attrName;
		this.val = val;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getVal(){
		return this.val;
	}
	
	public boolean equals(Attribute attr){
		if(this.getName().equals(attr.getName())
			&& this.getVal().equals(attr.getVal())){
			return true;
		}
		return false;
	}
	
	public String toString(){
		String attrString = "";
		
		attrString += "NAME :: " + this.getName() + "\n";
		attrString += "VALUE :: " + this.getVal() + "\n";
		
		return attrString;
	}
}
