package browsing;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Table;
import org.hibernate.annotations.Type;

@Table(appliesTo = "memories")
public class MemoryAccessor implements Serializable{
	
	private Long id;
	@Type(type = "serializable")
	private ConcurrentNode<?> node = null;
	
	public MemoryAccessor(ConcurrentNode<Page> node) {
		this.node = node;
	}
	
	@Id
	@Column(name="id")
	public Long getId(){
		return this.id;
	}
	
	
	@Column(name="compressedObject")
	public ConcurrentNode<?> getNode(){
		return node;
	}
	
	public void setNode(ConcurrentNode<?> node){
		this.node = node;
	}

}
