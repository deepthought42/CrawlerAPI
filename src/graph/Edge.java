package graph;

/**
 * Creates an edge linking 2 vertices based on their index within the {@link Graph} array
 * that the edge belongs to
 * 
 * @author Brandon Kindred
 *
 */
public class Edge {
	String label = "";
	long from = -1;
	long to = -1;
	
	public Edge(long from,long to){
		this.from = from;
		this.to = to;
	}
	
	public Edge(String label, long from, long to){
		this.label = label;
		this.from = from;
		this.to = to;
	}
	
	public long getFrom(){
		return this.from;
	}
	
	public long getTo(){
		return this.to;
	}
	
	public String getLabel(){
		return this.label;
	}
}
