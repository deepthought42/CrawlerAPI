package tester;

import graph.Graph;
import graph.Vertex;

import java.util.ArrayList;
import java.util.Iterator;

import browsing.ElementAction;
import browsing.Page;
import browsing.PageState;
import browsing.PathObject;
import structs.ConcurrentNode;
import structs.Path;

/**
 * Writes contents of testDefinition to a file.
 * 
 * @author Brandon Kindred
 */
public class TestWriter {
	private Path path = null;
	private String filename;
	
	public TestWriter(Path path, Graph graph){
		this.path = path;
	}

	/**
	 * Create a function that contains test statements for Quality Assurance
	 * @return
	 */
	public String createTest(){
		Iterator<PathObject> pathIterator = this.path.getPath().iterator();
		ArrayList<String> testStatements = new ArrayList<String>();
		while(pathIterator.hasNext()){
			Integer graph_idx = (Integer) pathIterator.next();
			Vertex<?> vertex = graph.getVertices().get(graph_idx);
			IStatementFactory statement = new ElementStatement();
			testStatements.add(statement.generateStatement(vertex.getData()));
			
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
	
	/**
	 * 
	 * @return
	 */
	public Path getRootNode(){
		return this.path;
	}
}
