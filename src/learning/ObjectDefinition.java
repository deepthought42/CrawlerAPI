package learning;

/**
 * Defines objects that are available to the system for learning against
 * 
 * @author Brandon Kindred
 *
 */
public class ObjectDefinition {

	public ObjectDefinition() {
		// TODO Auto-generated constructor stub
	}

	
	public long getId() {
		return id;
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

	private long id;
	private String type;
	private String name;
}
