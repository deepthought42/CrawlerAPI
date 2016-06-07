package tester;

import java.util.List;

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

	private final int key;
	public List<TestRecord> records;
	public final Path path;
	
	/**
	 * Constructs a test object
	 * 
	 * @param path {@link Path} that will be used to determine what the expected path should be
	 * 
	 * @pre path != null
	 */
	public Test(Path path){
		assert path != null;
		this.key = path.hashCode();
		this.path = path;
	}
	
	/**
	 * Returns test by key
	 * 
	 * @return
	 */
	public int getKey(){
		return this.key;
	}

	public Path getPath(){
		return this.path;
	}
	
	public void addTestRecord(TestRecord record){
		this.records.add(record);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Test){
			
		}
		
		return false;
	}
}
