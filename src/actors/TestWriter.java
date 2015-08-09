package actors;

import java.util.ArrayList;

import structs.ConcurrentNode;
import test.TestDefinition;

/**
 * Writes contents of testDefinition to a file.
 * @author Brandon Kindred
 *
 */
public class TestWriter {
	private ConcurrentNode<?> rootNode = null;
	private TestDefinition test = null;
	private String filename;
	
	public TestWriter(ConcurrentNode<?> startNode){
		this.rootNode = startNode;
		this.test = new TestDefinition();	 
	}
	
	public String getFileName(){
		return this.filename;
	}
	
	public void setFileName(String filename){
		this.filename = filename;
	}
	
	public TestDefinition getTest(){
		return this.test;
	}
	
	public ConcurrentNode<?> getRootNode(){
		return this.rootNode;
	}
}
