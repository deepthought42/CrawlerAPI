package actors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import akka.actor.UntypedActor;
import memory.DataDecomposer;
import memory.ObjectDefinition;
import memory.OrientDbPersistor;
import memory.PathNode;
import memory.Vocabulary;
import browsing.ActionFactory;
import browsing.Page;
import browsing.PathObject;
import structs.Message;
import structs.Path;
import tester.Test;
import tester.TestRecord;

/**
 * Handles the saving of records into orientDB
 * 
 * @author Brandon Kindred
 *
 */
public class MemoryRegistryActor extends UntypedActor{
    private static final Logger log = Logger.getLogger(BrowserActor.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			//Retains lists of productive, unproductive, and unknown value {@link Path}s.
			/*if(acct_msg.getData() instanceof Path){
				Path path = (Path)acct_msg.getData();
			
				OrientDbPersistor orient_persistor = new OrientDbPersistor();
				Vertex last_vertex = null;
				boolean last_id_set=false;
				int last_path_node_hash=0;
				String action = "contains";
				//orient_persistor.addVertexType(PathObject.class.getName());
				for(PathObject pathObj : path.getPath()){
					int objHash =  pathObj.data().hashCode();
					
					PathNode path_node = new PathNode(objHash, pathObj.data().getClass().getCanonicalName(), pathObj.data().toString());
					Vertex vertex = null;
					if(!pathObj.data().getClass().getCanonicalName().equals("browsing.actions.Action")){
						vertex = orient_persistor.findAndUpdateOrCreate(path_node, new String[0]);
					}
					else{
						action = pathObj.data().toString();
						continue;
					}
					
					if(last_id_set){
						Iterable<Edge> edges = orient_persistor.findEdges("hash_code", last_path_node_hash +""+ path_node.hash_code);
						Edge edge = null;
						
						if(edges.iterator().hasNext()){
							edge = edges.iterator().next();
						}
						else{
							edge = orient_persistor.addEdge(last_vertex, vertex, path_node.getClass().getCanonicalName(), last_path_node_hash +"-"+ path_node.hash_code);
							//edge.setProperty("path_uid", path_uuid);
							edge.setProperty("hash_code", last_path_node_hash +"-"+ path_node.hash_code);
							edge.setProperty("action", action);
							edge.setProperty("date", new Date());
						}
						
						if(path.isUseful() == null){
							edge.setProperty("value_status", "UNKNOWN");
						}
						else{
							edge.setProperty("value_status", path.isUseful());
						}
					}
					last_vertex = vertex;
					last_path_node_hash = path_node.hash_code;
					last_id_set = true;
				}
				
				orient_persistor.save();
			}
			else*/ if(acct_msg.getData() instanceof Page){
				Page page = (Page)acct_msg.getData();
				List<Object> decomposed_list = DataDecomposer.decompose(page);
				OrientDbPersistor persistor = new OrientDbPersistor();
				
				for(Object objDef : decomposed_list){
					//if object definition value doesn't exist in vocabulary 
					// then add value to vocabulary
					Vocabulary vocabulary = new Vocabulary(new ArrayList<String>(), "page");
					vocabulary.appendToVocabulary(((ObjectDefinition)objDef).getValue());
					persistor.findAndUpdateOrCreate(vocabulary, ActionFactory.getActions());
				}
			}
			else if(acct_msg.getData() instanceof Test){
				Test test = (Test)acct_msg.getData();

				//check if test with key already exists.
				OrientDbPersistor persistor = new OrientDbPersistor();
				Vertex vertex = persistor.findByKey(test.getKey());
				// if record already exists then create TestRecord and append it to records for test
				if(vertex != null){
					log.info("Test already exists....adding test as record");
					Test prev_test = (Test)vertex;					
					TestRecord record = new TestRecord(test.getPath(), new Date(), prev_test.getPath().equals(test.getPath()));
					prev_test.addTestRecord(record);
					
				}
				else{
					log.info("Saving test for the first time");
					Date date = new Date();
					TestRecord record = new TestRecord(test.getPath(), date, true);
					log.info("TEST RECORD :: "+record);
					log.info("TEST PATH :: "+test.getPath());
					log.info("DATE :: " + date);
					
					test.addTestRecord(record);
					persistor.findAndUpdateOrCreate(test);
					//persistor.addVertexType(record, OrientDbPersistor.getProperties(record));
					//persistor.createVertex(test);
					//persistor.save();
				}
			}
		}
		else unhandled(message);
	}
}
