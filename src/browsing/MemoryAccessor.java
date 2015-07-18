package browsing;
import java.io.Serializable;

public class MemoryAccessor implements Serializable{
	
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
