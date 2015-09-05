package learning;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * Defines objects that are available to the system for learning against
 * 
 * @author Brandon Kindred
 *
 */
@Entity
@Table( name = "objectDefinition" )
public class ObjectDefinition {

	public ObjectDefinition(Integer id, String name, String type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}

	public ObjectDefinition(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public Integer getId() {
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

	@Override
	public String toString(){
		return "ObjectDefinition: "+ this.id + ", " + this.type + ", " + this.name;

	}
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
	private Integer id;
	
	@Column(name = "type")
	private String type;
	
	@Column(name = "name")
	private String name;
}
