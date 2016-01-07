package shortTerm;

import java.util.ArrayList;
import java.util.HashMap;

import structs.Path;

/**
 * Retains lists of productive, unproductive, and unknown value {@link Path}s.
 * 
 * @author Brandon Kindred
 *
 */
public class ResultRegistry {
	private HashMap<Integer, ArrayList<Path>> productive_path_hash_queue = null;
	private HashMap<Integer, ArrayList<Path>> unproductive_path_hash_queue = null;
	private HashMap<Integer, ArrayList<Path>> unknown_outcome_path_hash_queue = null;
	
	public ResultRegistry() {
		productive_path_hash_queue = new HashMap<Integer, ArrayList<Path>>();
		unproductive_path_hash_queue = new HashMap<Integer, ArrayList<Path>>();
		unknown_outcome_path_hash_queue = new HashMap<Integer, ArrayList<Path>>();
	}
	
	/**
	 * Saves a path to the appropriate hash based on the 
	 * 
	 * @param path
	 * @param isValuable
	 */
	public synchronized void registerPath(Path path, Boolean isValuable){
		if(isValuable.equals(Boolean.TRUE)){
			registerProductivePath(path);
		}
		else if(isValuable.equals(Boolean.FALSE)){
			registerUnproductivePath(path);
		}
		else{
			registerUnknownOutcomePath(path);
		}
	}
	
	/**
	 * Used to inform the work allocator that a path was productive and has a positive value
	 * @param path
	 */
	private synchronized void registerProductivePath(Path path){
		ArrayList<Path> pathList = productive_path_hash_queue.get(path.getPath().toString());
		pathList.add(path);
		productive_path_hash_queue.put(path.getPath().size(), pathList);
	}
	
	/**
	 * Used to inform the work allocator that a path was productive and has a positive value
	 * @param path
	 */
	private synchronized void registerUnknownOutcomePath(Path path){
		ArrayList<Path> pathList = productive_path_hash_queue.get(path.getPath().toString());
		pathList.add(path);
		unknown_outcome_path_hash_queue.put(path.getPath().size(), pathList);
	}
	
	/**
	 * Used to inform the work allocator that a path was unproductive and has a negative value
	 * @param path
	 */
	private synchronized void registerUnproductivePath(Path path){
		ArrayList<Path> pathList = unproductive_path_hash_queue.get(path.getPath().toString());
		pathList.add(path);
		unproductive_path_hash_queue.put(path.getPath().size(), pathList);	
	}

}
