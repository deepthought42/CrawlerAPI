package structs;

import java.util.ArrayList;

/**
 * Represents the contents of a {@link Path} by holding the hashes of different
 * {@link Path} nodes instead of the actual data. Intended to reduce memory requirements
 * 
 * @author Brandon Kindred
 *
 */
public class PathRepresentation {
	private ArrayList<Integer> path_representation = null;
	
	/**
	 * Initializes a Path Represention
	 */
	public PathRepresentation() {
		path_representation = new ArrayList<Integer>();
	}
	
	/**
	 * Adds a hash value to the path representation
	 * 
	 * @param hash hash value of an {@link IBrowserObject}
	 */
	public void addToPath(int hash){
		this.path_representation.add(hash);
	}
	
	public ArrayList<Integer> getPathRepresentation(){
		return this.path_representation;
	}
	
	/**
	 * Returns the hash representations of each object in sequence as they are in this sequence.
	 */
	public String toString(){
		StringBuffer str_buf = new StringBuffer();
		for(Integer rep : path_representation){
			str_buf.append(rep);
		}
		
		return str_buf.toString();
	}
}
