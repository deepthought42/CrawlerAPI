package test;

/**
 * Interface for generating programming statements
 *  
 * @author Brandon Kindred
 *
 */
public interface IStatementGenerator {
	/**
	 * Generates a programming statement based input
	 * 
	 * @param o The object to be used for statement
	 * @return statement generated
	 */
	public String generateStatement(Object o);
	
	/**
	 * Generates a programming statement based on array of objects
	 * 
	 * @param o The object to be used for statement
	 * @return statement generated
	 */
	public String generateStatement(Object[] o);
}
