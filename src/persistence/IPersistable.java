package persistence;

public interface IPersistable<V> {
	public String generateKey();

	/**
	 * 
	 * @param framedGraph
	 */
	public V convertToRecord(OrientConnectionFactory connection);
	
	public IPersistable<V> create();
	
	/**
	 * Updates the given object by finding existing instances in the databases, making
	 * the appropriate updates, then saving the data to the database
	 * 
	 * @param existing_obj
	 * @return
	 */
	public IPersistable<V> update(V existing_obj);
	
	/**
	 * Use a key generated and guaranteed to be unique to retrieve all objects which have
	 * a "key" property value equal to the given generated key
	 * 
	 * @param generated_key
	 * @return
	 */
	public Iterable<V> findByKey(String generated_key);
}
