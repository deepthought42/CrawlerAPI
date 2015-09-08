package learning;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;


/**
 * Associates 2 {@link ObjectDefinition}s with each other and represents their
 * probabilistic association with each other
 * 
 * @author Brandon Kindred
 *
 */
@Entity
@Table(name="object_definition_associations")
@AssociationOverrides({
	@AssociationOverride(name = "pk.object1_id", 
		joinColumns = @JoinColumn(name = "object1_id")),
	@AssociationOverride(name = "pk.object2_id", 
		joinColumns = @JoinColumn(name = "object2_id")) })
public class ObjectDefinitionAssociation {
	
	public ObjectDefinitionAssociation() {

	}

	public ObjectDefinition getObjectDefintion_id() {
		return object1;
	}
	public void setObjectDefintion_id(ObjectDefinition objectDefintion) {
		object1 = objectDefintion;
	}

	public ObjectDefinition getObjectDefinition2_id() {
		return object2;
	}

	public void setObjectDefinition2_id(ObjectDefinition objectDefinition2) {
		object2 = objectDefinition2;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public long getCount() {
		return experience_count;
	}

	public void setCount(int count) {
		this.experience_count = count;
	}

	//@EmbeddedId
	//private StockCategoryId pk = new StockCategoryId();
	
	@ManyToOne
	@Column(name="object1_id")
	private ObjectDefinition object1;
	
	@ManyToOne
	@Column(name="object2_id")
	private ObjectDefinition object2;
	
	@Column(name="weight")
	private double weight;
	
	@Column(name="experience_count")
	private int experience_count;

}
