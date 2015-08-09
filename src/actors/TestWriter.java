package actors;

import structs.ConcurrentNode;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class TestWriter {
	private ConcurrentNode<?> rootNode = null;
	
	public TestWriter(ConcurrentNode<?> startNode){
		this.rootNode = startNode;
	}
	
	
}
