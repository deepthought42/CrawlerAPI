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
	int from = -1;
	int to = -1;
	
	public Edge(int from,int to){
		this.from = from;
		this.to = to;
	}
	
	public Edge(String label, int from, int to){
		this.label = label;
		this.from = from;
		this.to = to;
	}
	
	public int getFrom(){
		return this.from;
	}
	
	public int getTo(){
		return this.to;
	}
	
	public String getLabel(){
		return this.label;
	}
}
