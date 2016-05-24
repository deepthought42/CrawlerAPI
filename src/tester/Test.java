package tester;

import org.apache.log4j.Logger;

import structs.Path;

/**
 * Defines the path of a test, the result and the expected values to determine if a test was 
 * successful or not
 * 
 * @author Brandon Kindred
 *
 */
public class Test {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Test.class);

	public final boolean resultingUsefulness;
	public Boolean isCorrect;
	public final Path path;
	
	/**
	 * Constructs a test object
	 * 
	 * @param resultingUsefulness
	 * @param path
	 */
	public Test(boolean resultingUsefulness, Path path){
		this.resultingUsefulness = resultingUsefulness;
		this.isCorrect = null;
		this.path = path;
	}
	
	/**
	 * Sets the correctness of the test. If the resulting path deviates from the original 
	 * path then it is incorrect
	 * 
	 * @param isCorrect
	 */
	public void setIsCorrect(boolean isCorrect){
		this.isCorrect = isCorrect;
	}
	
	public boolean getIsCorrect(){
		return this.isCorrect;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Test){
			
		}
		
		return false;
	}
}
