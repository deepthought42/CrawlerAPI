package learning;

import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;


/**
 * Defines objects that are available to the system for learning against
 * 
 * @author Brandon Kindred
 *
 */
@Entity
@Table( name = "objectDefinition" )
public class ObjectDefinition {

	public ObjectDefinition(Long id, String name, String type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}

	public ObjectDefinition(String name, String type) {
		this.name = name;
		this.type = type;
	}
	
	public ObjectDefinition(){}

	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
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
		return "ObjectDefinition: "+ this.id + ", " + this.type + ", " + this.name;

	}
	
	@Id 
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	@Column(name = "id")
	private Long id;
	
	@Column(name = "type")
	private String type;
	
	@Column(name = "name")
	private String name;
}
