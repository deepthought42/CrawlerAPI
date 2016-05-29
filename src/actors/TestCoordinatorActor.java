package actors;

import java.net.URL;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

import akka.actor.UntypedActor;
import browsing.Browser;
import browsing.Page;
import browsing.PageElement;
import browsing.actions.Action;
import memory.OrientDbPersistor;
import structs.Message;
import structs.Path;

/**
 * Handles retrieving tests
 * 
 * @author brandon kindred
 *
 */
public class TestCoordinatorActor extends UntypedActor {
    private static final Logger log = Logger.getLogger(WorkAllocationActor.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			if(acct_msg.getData() instanceof URL){
				URL url = (URL)acct_msg.getData();
				//Retrieve from memory
				Browser browser = new Browser(url.toString());
				Page page = browser.getPage();
				
				OrientDbPersistor persistor = new OrientDbPersistor();
				Iterator<Vertex> page_iter = persistor.findVertices(page).iterator();
				
				Path path = new Path();
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
							path.add((Action)result_vertices.next());
						}
					}
				}
				
				//load all edges from pageElements
			}
			else{
				log.info("ERROR : Message contains unknown format");
			}
		}
		else{
			log.info("ERROR : Did not receive a Message object");
		}
	}

}
