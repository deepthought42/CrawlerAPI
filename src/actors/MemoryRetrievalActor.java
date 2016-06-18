package actors;

import java.util.ArrayList;
import java.util.Iterator;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

import akka.actor.UntypedActor;
import memory.OrientDbPersistor;
import memory.Vocabulary;
import browsing.Page;
import browsing.PageElement;
import browsing.PathObject;
import structs.Message;
import structs.Path;
import tester.Test;

/**
 * Retains lists of productive, unproductive, and unknown value {@link Path}s.
 * 
 * @author Brandon Kindred
 *
 */
public class MemoryRetrievalActor extends UntypedActor{
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		//Get all edges from page and from next level nodes
		if(message instanceof Page){
			Page page_msg = (Page)message;
			OrientDbPersistor persistor = new OrientDbPersistor();
			Iterator<Vertex> page_iter = persistor.findVertices(page_msg).iterator();
			Page page = (Page)page_iter.next();

			Message<Page> msg = new Message<Page>(null, page);
			
			//send path to actor that handles running tests
		}
		else if(message instanceof Path){
			Path path = (Path)message;
			//Retrieve from memory
			
			OrientDbPersistor persistor = new OrientDbPersistor();
			Iterator<Vertex> page_iter = persistor.findVertices("src", ((Page)path.getPath().get(0).data()).getSrc()).iterator();
			
			//load all edges that leading to pageElement
			while(page_iter.hasNext()){
				Vertex page_vert = page_iter.next();
				path.add((Page)page_vert);
				Iterator<Vertex> page_element_iter = page_vert.getVertices(Direction.OUT, "Page").iterator();
				while(page_element_iter.hasNext()){
					Vertex page_element_vertex = page_element_iter.next();
					path.add((PageElement)page_element_vertex);
					
					Iterator<Vertex> result_vertices = page_element_vertex.getVertices(Direction.OUT, "PageElement").iterator();
					
					while(result_vertices.hasNext()){
						path.add(new PathObject<Vertex>(result_vertices.next()));
					}
				}
			}
			
			//send path to actor that handles running tests
		}
		else if(message instanceof Test){
			Test test = (Test)message;
			//Retrieve from memory
			
			OrientDbPersistor persistor = new OrientDbPersistor();
			Iterator<Vertex> page_iter = persistor.findVertices("src", ((Page)path.getPath().get(0).data()).getSrc()).iterator();
			Test stored_test = (Test)page_iter.next();
			Message<Test> msg = new Message<Test>(null, test);
			//send path to actor that handles running tests
		}
		else if(message instanceof Vocabulary){
			//retrieve all vocabulary values from memory
			OrientDbPersistor persistor = new OrientDbPersistor();
			Iterable<Vertex> vertex = persistor.findVertices("vocabulary", "page");
			//if more than one vertex is available 
			//  then merge the vertices though learning, delete all vertices and persist the merged verges
			while(vertex.iterator().hasNext()){
				Vertex v = vertex.iterator().next();
				ArrayList<String> vocab = v.getProperty("vocabulary");
				Vocabulary vocabulary = new Vocabulary(vocab, "page");
				
				//pass vocabulary to appropriate actor
				
			}
		}
		else unhandled(message);
		
	}
}
