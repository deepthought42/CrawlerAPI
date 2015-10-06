package memory;

/**
 * Defines objects that are available to the system for learning against
 * 
 * @author Brandon Kindred
 *
 */
public class ObjectDefinition {

	public ObjectDefinition(int count, String name, String type) {
		this.count = count;
		this.name = name;
		this.type = type;
	}

	public ObjectDefinition(String name, String type) {
		this.count = 1;
		this.name = name;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString(){
		return "ObjectDefinition: "+ this.count + ", " + this.type + ", " + this.name;

	}
	
	private int count;
	private String type;
	private String name;
}
