package graph;

import structs.ConcurrentNode;
import structs.Path;

/**
 * Traces previous nodes to root node
 * 
 * @author Brandon Kindred
 *
 */
public class PathTracer {
	
	public static Path traceToRoot(ConcurrentNode<?> node){
		node.getInput(node.getUuid());
		return null;		
	}
}
