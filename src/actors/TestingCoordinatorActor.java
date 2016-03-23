package actors;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openqa.selenium.WebElement;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import browsing.PathObject;
import browsing.actions.Action;
import memory.OrientDbPersistor;
import memory.PathNode;
import structs.Path;

/**
 * An {@link Actor} that is responsible for retrieving tests from the database and
 * assigning the discovered tests to a {@link BrowserActor} for verification of expected results
 * 
 * @author brandon kindred
 *
 */
public class TestingCoordinatorActor implements Actor, Callable<Object> {
	public final UUID uuid;
	
	public TestingCoordinatorActor(){
		this.uuid = UUID.randomUUID();
	}
	
	/**
	 * 
	 */
	public Object call() throws Exception{
		OrientDbPersistor<PathNode> persistor = new OrientDbPersistor<PathNode>();
		//Initialize new path
		Path path = new Path();
		boolean wasChangeExpected = false;
		//Retrieve all entry nodes for a given domain from orientDB
		Iterable<Vertex> vertices = persistor.findVertices("url", "ideabin.io");
		//Iterable<Vertex> vertices = persistor.graph.getVertices("url", null);
		//get all edges for root node
		for(Vertex vertex: vertices){
			Iterable<Edge> edges = vertex.getEdges(Direction.OUT, "");
			path.add(new PathObject<Vertex>(vertex));
			
			for(Edge edge : edges){
				path.add(new PathObject<Edge>(edge));
				//for each node that is a webElement get node connections
				Vertex next_vertex = edge.getVertex(Direction.OUT);
				String class_value = next_vertex.getProperty("class");
				if(class_value.equals(WebElement.class.getSimpleName())){
					path.add(new PathObject<Vertex>(next_vertex));
					
					}
					Iterable<Edge> element_edges = next_vertex.getEdges(Direction.OUT, "");
					for(Edge element_edge: element_edges){
						class_value = element_edge.getProperty("class");
						path.add(new PathObject<Edge>(element_edge));
						Vertex action_vertex =  element_edge.getVertex(Direction.OUT);
						//check if it is an action being performed
						if(class_value.equals(Action.class.getSimpleName())){
							//add to Path
							path.add(new PathObject<Vertex>(action_vertex));

						}
					}
				}
			}
		
		//Create BrowserActor and give it the path
		ExecutorService es = Executors.newSingleThreadExecutor();
		
		//Start browser actor
		Future<Boolean> actorResponse = es.submit(new BrowserActor(path));
		boolean didChangeOccur = false;
		try {
			//Wait for response from Actor
			didChangeOccur = actorResponse.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Check that actor gets same result that was expected based on last result
		if(didChangeOccur==wasChangeExpected){
			//mark as passing
		}
		else{
			//mark as failing
		}

		//
		// record the results in database
		
		return null;
	}
	
	/**
	 * 
	 * @param path
	 * @param didPass
	 */
	public static void registerResponse(Path path, Boolean didPass, Boolean expectedPass){
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public UUID getActorId() {
		return uuid;
	}

}
