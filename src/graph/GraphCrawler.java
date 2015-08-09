package graph;

public interface GraphCrawler {
	public boolean putNodesOnFrontier();
	
	public boolean putNodeInVisited();
	
	public boolean removeNodeFromFrontier();
	
	public int findNodeInVisited();
}
