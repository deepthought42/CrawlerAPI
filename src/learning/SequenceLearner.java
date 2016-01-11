package learning;

import graph.Graph;
import graph.Vertex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import memory.DataDecomposer;
import memory.MemoryState;
import memory.ObjectDefinition;
import memory.Persistor;
import browsing.Page;
import browsing.PageElement;

import com.tinkerpop.blueprints.Edge;

public class SequenceLearner {
	
	private Persistor persistor = null;
	private Graph graph = null;
	
	public SequenceLearner() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Reads path and performs learning tasks
	 * 
	 * @param path an {@link ArrayList} of graph vertex indices. Order matters
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NullPointerException
	 * @throws IOException 
	 */
	public void learn(Page last_page, Page current_page, PageElement last_element, String last_action) throws IllegalArgumentException, IllegalAccessException, NullPointerException, IOException{
		//REINFORCEMENT LEARNING
		System.out.println( " Initiating learning");

		MemoryState memState = new MemoryState(last_page.hashCode());
		com.tinkerpop.blueprints.Vertex state_vertex = null;
		try{
			state_vertex = memState.createAndLoadState(last_page, null, persistor);
		}catch(IllegalArgumentException e){}
		

		double actual_reward = 0.0;
	
		if(!last_page.equals(current_page)){
			actual_reward = 1.0;
			
			com.tinkerpop.blueprints.Vertex new_state_vertex = null;
			MemoryState new_memory_state = new MemoryState(current_page.hashCode());
			
			new_state_vertex = new_memory_state.createAndLoadState(current_page, state_vertex, persistor);

			//add it to in memory map. This should be changed to use some sort of caching
			Vertex<?> vertex = new Vertex<Page>(current_page);
			graph.addVertex(vertex);
			int idx = graph.findVertexIndex(vertex);
			path.add(idx);
			
			workAllocator.registerProductivePath(path);
			//putPathOnQueue(path);
			//add new edge to memory
			
			if(!state_vertex.equals(new_state_vertex)){
				System.out.println("Adding GOES_TO transition");
				Edge e = persistor.addEdge(state_vertex, new_state_vertex, "TRANSITION", "GOES_TO");
				e.setProperty("action", last_action);
				e.setProperty("xpath", last_element.xpath);
			}
			System.err.println("SAVING NOW...");
			persistor.save();
		}
		else{
			//nothing changed so there was no reward for that combination. We want to remember this in the future
			// so we set it to a negative value to simulate regret
			actual_reward = -1.0;
			workAllocator.registerUnproductivePath(originalPath);
		}
		
		//get all objects for the chosen page_element
		DataDecomposer mem = new DataDecomposer(last_element);
		List<ObjectDefinition> best_definitions = mem.decompose();
		System.err.println("TOTAL BEST DEFINTIONS :: " + best_definitions.size());
		//Q-LEARNING VARIABLES
		final double learning_rate = .08;
		final double discount_factor = .08;
		
		//machine learning algorithm should produce this value
		double estimated_reward = 1.0;
		
		QLearn q_learn = new QLearn(learning_rate, discount_factor);
		//Reinforce probabilities for the component objects of this element
		for(ObjectDefinition objDef : best_definitions){
			HashMap<String, Double> action_map = objDef.getActions();
			
			//NEED TO LOOK UP OBJECT DEFINITION IN MEMORY, IF IT EXISTS, THEN IT SHOULD BE LOADED AND USED, 
			//IF NOT THEN IT SHOULD BE CREATED POPULATED AND SAVED
			Iterator<com.tinkerpop.blueprints.Vertex> v_mem_iter = persistor.find(objDef).iterator();
			com.tinkerpop.blueprints.Vertex memory_vertex = null;
			if(v_mem_iter.hasNext()){
				memory_vertex = v_mem_iter.next();
				action_map = memory_vertex.getProperty("actions");
				if(action_map == null){
					action_map = objDef.getActions();
				}
			}
			double last_reward = 0.0;

			if(action_map.containsKey(last_action)){
				System.out.println("Last action : "+last_action + " exists in action_map for object");
				last_reward = action_map.get(last_action);
			}
			
			System.err.println("last reward : "+last_reward);
			System.err.println("actual_reward : "+actual_reward);
			System.err.println("estimated_reward : "+estimated_reward);
			
			double q_learn_val = q_learn.calculate(last_reward, actual_reward, estimated_reward );
			action_map.put(last_action, q_learn_val);
			System.err.println(this.getName() + " -> ADDED LAST ACTION TO ACTION MAP :: "+last_action+"...Q LEARN VAL : "+q_learn_val);

			
			objDef.setActions(action_map);
			com.tinkerpop.blueprints.Vertex v = objDef.findAndUpdateOrCreate(persistor);
		}
	}

}
