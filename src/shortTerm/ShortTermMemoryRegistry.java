package shortTerm;

import java.util.HashMap;

import structs.Path;

/**
 * Retains lists of productive, unproductive, and unknown value {@link Path}s.
 * 
 * @author Brandon Kindred
 *
 */
public class ShortTermMemoryRegistry {
	private HashMap<String, Path> productive_path_hash_queue = null;
	private HashMap<String, Path> unproductive_path_hash_queue = null;
	private HashMap<String, Path> unknown_outcome_path_hash_queue = null;
	
	public ShortTermMemoryRegistry() {
		productive_path_hash_queue = new HashMap<String, Path>();
		unproductive_path_hash_queue = new HashMap<String, Path>();
		unknown_outcome_path_hash_queue = new HashMap<String, Path>();
	}
	
	/**
	 * Saves a path to the appropriate hash based on the 
	 * 
	 * @param path
	 * @param isValuable
	 */
	public synchronized void registerPath(Path path, Boolean isValuable){
		if(isValuable == null){
			registerUnknownOutcomePath(path);
		}
		else if(isValuable.equals(Boolean.TRUE)){
			registerProductivePath(path);
		}
		else if(isValuable.equals(Boolean.FALSE)){
			registerUnproductivePath(path);
		}
	}
	
	/**
	 * Used to inform the work allocator that a path was productive and has a positive value
	 * 
	 * @param path
	 */
	private synchronized void registerProductivePath(Path path){
		boolean exists = productive_path_hash_queue.containsKey(path.getPath().toString());
		if(!exists){
			productive_path_hash_queue.put(path.getPath().toString(), path);
			System.err.println("PRODUCTIVE PATH REGISTERED :: "+path.getPath().toString());

		}
	}
	
	/**
	 * Used to inform the work allocator that a path was productive and has a positive value
	 * 
	 * @param path
	 */
	private synchronized void registerUnknownOutcomePath(Path path){
		boolean exists = unknown_outcome_path_hash_queue.containsKey(path.getPath().toString());
		if(!exists){
			unknown_outcome_path_hash_queue.put(path.getPath().toString(), path);
			System.err.println("UNKNOWN PATH REGISTERED :: "+path.getPath().toString());
		}
	}
	
	/**
	 * Used to inform the work allocator that a path was unproductive and has a negative value
	 * 
	 * @param path
	 */
	private synchronized void registerUnproductivePath(Path path){
		boolean exists = unproductive_path_hash_queue.containsKey(path.getPath().toString());
		if(!exists){
			unproductive_path_hash_queue.put(path.getPath().toString(), path);
			System.err.println("UNPRODUCTIVE PATH REGISTERED :: "+path.getPath().toString());

		}
	}

	/**
	 * 
	 * @return
	 */
	public HashMap<String, Path> getUnknownPaths() {
		return this.unknown_outcome_path_hash_queue;
	}

	/**
	 * 
	 * @return
	 */
	public HashMap<String, Path> getProductivePaths(){
		return this.productive_path_hash_queue;
	}
	
	/**
	 * 	
	 * @return
	 */
	public HashMap<String, Path> getUnproductivePaths(){
		return this.unproductive_path_hash_queue;
	}
}
