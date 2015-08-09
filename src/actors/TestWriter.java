package actors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import structs.ConcurrentNode;
import test.TestDefinition;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class TestWriter {
	private ConcurrentNode<?> rootNode = null;
	private ArrayList<TestDefinition> tests = null;
	
	public TestWriter(ConcurrentNode<?> startNode){
		this.rootNode = startNode;
		this.tests = new ArrayList<TestDefinition>();	 
	}
}
