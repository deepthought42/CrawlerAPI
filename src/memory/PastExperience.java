package memory;

import java.util.ArrayList;
import java.util.Date;

import java.util.UUID;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import browsing.PathObject;
import structs.Path;

/**
 * Handles the creation and maintenance of a "ledger" of past experiences saved to disk
 * 
 * @author Brandon Kindred
 *
 */
public class PastExperience {
	private ArrayList<Path> paths = null;
	private int value = 0;
	private Boolean useful = null;
	
	public PastExperience() {
		paths = new ArrayList<Path>();
	}
	
	public void appendToPaths(Path path){
		this.paths.add(path);
	}
	
	/**
	 * Appends a path to the end of a path record
	 * 
	 * @param path	{@link Path} to append to list
	 * @param isValuable indicator of if path is seen as valuable, invaluable, or unknown value
	 */
	public void appendToPaths(Path path, Boolean isValuable){
		this.paths.add(path);
		
		OrientDbPersistor<PathNode> orient_persistor = new OrientDbPersistor<PathNode>();
		UUID path_uuid = UUID.randomUUID();
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
				
				if(isValuable == null){
					edge.setProperty("value_status", "UNKNOWN");
				}
				else{
					edge.setProperty("value_status", isValuable);
				}
			}
			last_vertex = vertex;
			last_path_node_hash = path_node.hash_code;
			last_id_set = true;
		}
		
		orient_persistor.save();
	}
	
	public ArrayList<Path> getPaths(){
		return this.paths;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public Boolean getUseful() {
		return useful;
	}

	public void setUseful(Boolean useful) {
		this.useful = useful;
	}
}
