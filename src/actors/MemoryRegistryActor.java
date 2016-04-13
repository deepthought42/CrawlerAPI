package actors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import structs.Path;

/**
 * Retains lists of productive, unproductive, and unknown value {@link Path}s.
 * 
 * @author Brandon Kindred
 *
 */
public class MemoryRegistryActor extends UntypedActor{
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Path){
			Path path = (Path)message;
			//save to memory
			
			OrientDbPersistor<PathNode> orient_persistor = new OrientDbPersistor<PathNode>();
			Vertex last_vertex = null;
			boolean last_id_set=false;
			int last_path_node_hash=0;
			String action = "contains";
			//orient_persistor.addVertexType(PathObject.class.getName());
			for(PathObject<?> pathObj : path.getPath()){
				int objHash =  pathObj.getData().hashCode();
				
				PathNode path_node = new PathNode(objHash, pathObj.getData().getClass().getCanonicalName(), pathObj.getData().toString());
				Vertex vertex = null;
				if(!pathObj.getData().getClass().getCanonicalName().equals("browsing.actions.Action")){
					vertex = orient_persistor.findAndUpdateOrCreate(path_node, new String[0]);
				}
				else{
					action = pathObj.getData().toString();
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
		else if(message instanceof Page){
			Page page = (Page)message;
			List<ObjectDefinition> decomposed_list = DataDecomposer.decompose(page);
			OrientDbPersistor<Vocabulary> persistor = new OrientDbPersistor<Vocabulary>();
			
			for(ObjectDefinition objDef : decomposed_list){
				//if object definition value doesn't exist in vocabulary 
				// then add value to vocabulary
				Vocabulary vocabulary = new Vocabulary(new ArrayList<String>(), "page");
				vocabulary.appendToVocabulary(objDef.getValue());
				persistor.findAndUpdateOrCreate(vocabulary, ActionFactory.getActions());
			}
		}
		else unhandled(message);
		
	}
}
