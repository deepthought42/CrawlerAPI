package learning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import structs.Path;
import structs.PathRepresentation;
import memory.DataDecomposer;
import memory.MemoryState;
import memory.ObjectDefinition;
import memory.OrientDbPersistor;
import memory.Vocabulary;
import browsing.Page;
import browsing.PageElement;
import browsing.PathObject;
import browsing.actions.Action;

import com.tinkerpop.blueprints.Edge;

public class Learner {
	
	private OrientDbPersistor<Page> persistor = null;
	private Vocabulary vocabulary = null;
	
	public Learner(Vocabulary vocab) {
		this.vocabulary = vocab;
	}
	
	public Learner(){
		this.vocabulary = new Vocabulary(new ArrayList<String>(), "global");
	}

	public void qLearn(){
		
	}
	
	public double[] predict(){
		return new double[0];
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
	public void learn(Path path,
					  boolean isProductive)
						  throws IllegalArgumentException, IllegalAccessException, 
							  NullPointerException, IOException{
		//REINFORCEMENT LEARNING
		System.out.println( " Initiating learning");

		//DUE TO CHANGES IN ARCHITECTURE THE WAY THAT LEARNING WILL OCCUR WILL BE DIFFERENT THAN THE ORIGINAL LOGIC
		
		//Iterate over path objects
		//if previous pathObject is not an action and the current pathObject is also not an action
		//	then create a component edge between both pathObjects
		//else if current pathObject is an action, 
		//	then 
		//		extract action
		//		get next pathObject and previous pathObject 
		//		create action edge between the current and previous pathObject
		//		set edge property for action to the action that was extracted from path
		
		PathObject<?> prev_obj = null;
		for(PathObject<?> obj : path.getPath()){
			List<ObjectDefinition> decomposer = DataDecomposer.decompose(obj.getData());

			for(ObjectDefinition objDef : object_definition_list){
				//if object definition value doesn't exist in vocabulary 
				// then add value to vocabulary
				vocabulary.appendToVocabulary(objDef.getValue());
			}
			
			//Save states
			/** Handled already in memory Registry I think...LEAVE THIS UNTIL VERIFIED ITS NOT NEEDED

			if(prev_obj != null && !(prev_obj.getData() instanceof Action)){
				Vertex prev_vertex = persistor.find(prev_obj);
				Vertex current_vertex = persistor.find(obj);
				ArrayList<Integer> path_ids = new ArrayList<Integer>();
				path_ids.add(path.hashCode());
				System.out.println("Adding GOES_TO transition");
				Edge e = persistor.addEdge(prev_vertex, current_vertex, "Component", "GOES_TO");
				e.setProperty("path_ids", path_ids);
			}
			else if(prev_obj != null && prev_obj.getData() instanceof Action){
				Vertex prev_vertex = persistor.find(prev_obj);
				Vertex current_vertex = persistor.find(obj);
				ArrayList<Integer> path_ids = new ArrayList<Integer>();
				path_ids.add(path.hashCode());
				System.out.println("Adding GOES_TO transition");
				Edge e = persistor.addEdge(prev_vertex, current_vertex, "Transition", "GOES_TO");
				e.setProperty("path_ids", path_ids);
			}
			
			
			
			System.err.println("SAVING NOW...");
			persistor.save();
			*/
			
			prev_obj = obj;
		}
		
		
		
		
		
		
		MemoryState memState = new MemoryState(last_page.hashCode());
		com.tinkerpop.blueprints.Vertex state_vertex = null;
		try{
			state_vertex = memState.createAndLoadState(last_page, null, persistor);
		}catch(IllegalArgumentException e){}
		

		double actual_reward = 0.0;
	
		if(!last_page.equals(current_page)){
			actual_reward = 10.0;
			
			com.tinkerpop.blueprints.Vertex new_state_vertex = null;
			MemoryState new_memory_state = new MemoryState(current_page.hashCode());
			
			new_state_vertex = new_memory_state.createAndLoadState(current_page, state_vertex, persistor);

			//w edge to memory
			
		}
		else{
			//nothing changed so there was no reward for that combination. We want to remember this in the future
			// so we set it to a negative value to simulate regret
			actual_reward = -1.0;
		}
		
		//get all objects for the chosen page_element
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
			System.err.println(" -> ADDED LAST ACTION TO ACTION MAP :: "+last_action+"...Q LEARN VAL : "+q_learn_val);

			objDef.setActions(action_map);
			com.tinkerpop.blueprints.Vertex v = objDef.findAndUpdateOrCreate(persistor);
		}
	}

}
