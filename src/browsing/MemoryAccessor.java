package browsing;
import java.io.Serializable;

import structs.ConcurrentNode;

public class MemoryAccessor implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1066556158468096508L;
	private Long id;
	private ConcurrentNode<?> node = null;
	
	public MemoryAccessor(ConcurrentNode<Page> node) {
		this.node = node;
	}

	public Long getId(){
		return this.id;
	}
	
		public ConcurrentNode<?> getNode(){
		return node;
	}
	
	public void setNode(ConcurrentNode<?> node){
		this.node = node;
	}

}
