package actors;

import java.util.ArrayList;
import java.util.Iterator;

import browsing.ElementAction;
import browsing.Page;
import browsing.PageState;
import structs.ConcurrentNode;
import structs.Path;
import test.TestDefinition;

/**
 * Writes contents of testDefinition to a file.
 * @author Brandon Kindred
 *
 */
public class TestWriter {
	private Path path = null;
	private TestDefinition test = null;
	private String filename;
	
	public String createTest(){
		Iterator<?> pathIterator = this.path.getPath().iterator();
		while(pathIterator.hasNext()){
			ConcurrentNode<?> pathNode = (ConcurrentNode<?>) pathIterator.next();
			//Determine nodeType and modify test defintion accordingly
			if(pathNode.getClass().equals(ElementAction.class)){
				//if element action
				
			}
			else if(pathNode.getClass().equals(PageState.class)){
				
			}
			else if(pathNode.getClass().equals(Page.class)){
				
			}
			
		}
		return null;
	}
	
	public TestWriter(Path path){
		this.path = path;
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
	
	public Path getRootNode(){
		return this.path;
	}
}
