package test;

import java.util.ArrayList;

/**
 * Interface for generating programming statements
 *  
 * @author Brandon Kindred
 *
 */
public interface IStatementFactory {
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
