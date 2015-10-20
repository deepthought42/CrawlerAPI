package memory;

/**
 * Defines objects that are available to the system for learning against
 * 
 * @author Brandon Kindred
 *
 */
public class ObjectDefinition {

	private String value;
	private int count;
	private String type;
	private double probability;
	
	public ObjectDefinition(int count, String value, String type) {
		this.count = count;
		this.value = value;
		this.type = type;
	}

	public ObjectDefinition(String name, String type) {
		this.count = 1;
		this.type = type;
	}
	
	public ObjectDefinition(){}

	public int getCount() {
		return count;
	}
	
	private void setCount(int count) {
		this.count = count;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue(){
		return this.value;
	}
	
	@Override
	public String toString(){
		return this.value;

	}

	public void incrementCount() {
		this.count += 1;
	}

	public void setProbability(double i) {
		this.probability = i;		
	}
	
	public double getProbability(){
		return this.probability;
	}
}
