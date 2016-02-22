package browsing;

/**
 * Provides a wrapper for implementing objects and provides the ability to get cost and reward values
 * 
 * @author Brandon Kindred
 *
 */
public class PathObject<T> {
	T pathObject = null;
	
	
	public PathObject(T object){
		this.pathObject = object;
	}
	
	/**
	 * Returns wrapped object
	 * @return
	 */
	public T getData(){
		return this.pathObject;
	}
}
