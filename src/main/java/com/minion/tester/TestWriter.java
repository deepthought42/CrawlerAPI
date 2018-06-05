package com.minion.tester;

import java.util.ArrayList;
import java.util.List;

import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Test;


/**
 * Writes contents of testDefinition to a file.
 */
public class TestWriter {
	private Test test = null;
	private String filename;
	
	/*public TestWriter(Path path, Graph graph){
		this.path = path;
	}
*/
	/**
	 * Create a function that contains test statements for Quality Assurance
	 * @return
	 */
	public String createTest(){
		List<PathObject> pathIterator = test.getPathObjects();
		ArrayList<String> testStatements = new ArrayList<String>();
		for(PathObject obj : pathIterator){
			//Integer graph_idx = (Integer) pathIterator.next();
			//Vertex<?> vertex = graph.getVertices().get(graph_idx);
			IStatementFactory statement = new ElementStatement();
			//testStatements.add(statement.generateStatement(vertex.getData()));
			
		}	
		
		//THIS IS NOT AN ACTUAL IMPLEMENTATION. JUST A PLACEHOLDER. DELETE COMMENT WHEN ACTUALLY REPLACED.
		String test = "";
		for(String statement: testStatements){
			test += statement+"\n";
		}
		return test;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getFileName(){
		return this.filename;
	}
	
	/**
	 * 
	 * @param filename
	 */
	public void setFileName(String filename){
		this.filename = filename;
	}

}
