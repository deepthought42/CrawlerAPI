package com.qanairy.rl.learning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.minion.browsing.ActionFactory;
import com.minion.browsing.actions.Action;
import com.minion.persistence.OrientConnectionFactory;
import com.minion.structs.Path;
//import com.minion.structs.Path;
import com.qanairy.rl.memory.DataDecomposer;
import com.qanairy.rl.memory.MemoryState;
import com.qanairy.rl.memory.ObjectDefinition;
import com.qanairy.rl.memory.Vocabulary;
import com.qanairy.rl.memory.OrientDbPersistor;
//import com.minion.browsing.Page;
//import com.minion.browsing.PageElement;
//import com.minion.browsing.PathObject;
//import com.minion.browsing.actions.Action;
import com.qanairy.rl.memory.OrientRLMemoryConnectionFactory;

//import com.tinkerpop.blueprints.Edge;

/**
 *
 */
public class Brain {
	
	private OrientDbPersistor persistor = null;
	
	public static double[] predict(List<ObjectDefinition> object_definitions, String[] actions){
		// 1. identify vocabulary (NOTE: This is currently hard coded since we only currently care about 1 context)
		Vocabulary vocabulary = new Vocabulary(new ArrayList<String>(), "internet");
		// 2. create record based on vocabulary
		for(ObjectDefinition objDef : object_definitions){
			vocabulary.appendToVocabulary(objDef.getValue());
			//load policy for object definition
		}
		
		// 3. adjust action policies if more actions exist than the known actions
		String[] known_actions = ActionFactory.getActions();
		
		// 4. load known vocabulary action policies into matrix
		// 5. generate vocabulary policy matrix from list of object_definitions

		double[][] vocab_actions = new double[vocabulary.getValueList().size()][known_actions.length];
		
		int idx = 0;
		for(String vocab_word : vocabulary.getValueList()){
			boolean[] state = new boolean[vocabulary.getValueList().size()];
			if(object_definitions.contains(vocab_word)){
				state[idx] = true;
				//load vocabulary object definition and make sure action list of probabilities is in the proper order

				for(int action_idx=0; action_idx<known_actions.length; action_idx++){
					//vocab_actions[idx][action_idx] = 0.1;  whats the dynamic value though??
				}
			}
			else{
				state[idx] = false;
				for(int action_idx=0; action_idx<known_actions.length; action_idx++){
					vocab_actions[idx][action_idx] = 0.0;
				}
			}
			idx++;
		}
		
		// 5. run predictions with the vocabulary vector as a record and multiply it by the action policies st. vocabulary_state*(policies)^Transpose = Y
		// 6. return predicted action vector
		
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
	public static void learn(List<ObjectDefinition> object_definition_list,
					  Map<String,Double> predicted_action_vector,
					  String actual_action,
					  boolean isProductive)
						  throws IllegalArgumentException, IllegalAccessException, 
							  NullPointerException, IOException{
		//REINFORCEMENT LEARNING
		System.out.println( " Initiating learning");
		
		//learning model
		// 1. identify vocabulary (NOTE: This is currently hard coded since we only currently care about 1 context)
		Vocabulary vocabulary = new Vocabulary(new ArrayList<String>(), "internet");

		// 2. create record based on vocabulary
		for(ObjectDefinition objDef : object_definition_list){
			vocabulary.appendToVocabulary(objDef.getValue());
		}
		
		// 2. create state vertex from vocabulary and
		int idx = 0;
		for(String vocab_word : vocabulary.getValueList()){
			boolean[] state = new boolean[vocabulary.getValueList().size()];
			if(object_definition_list.contains(vocab_word)){
				state[idx] = true;
			}
			else{
				state[idx] = false;
			}
			idx++;
		}
		
		// 2a. load known action policies/probabilities for each object definition in the definition

		// 3. determine reward/regret score based on productivity status
		double actual_reward = 0.0;
		
		if(isProductive){
			actual_reward = 10.0;
		}
		else{
			//nothing changed so there was no reward for that combination. We want to remember this in the future
			// so we set it to a negative value to simulate regret
			actual_reward = -1.0;
		}
		
		// 4. apply reward/regret to predicted_action_vector
		// 5. perform backpropagation through network supporting result
		//Q-LEARNING VARIABLES
		final double learning_rate = .08;
		final double discount_factor = .08;
		
		//machine learning algorithm should produce this value
		double estimated_reward = 1.0;
		
		QLearn q_learn = new QLearn(learning_rate, discount_factor);
		
		//Reinforce probabilities for the component objects of this element
		for(ObjectDefinition objDef : object_definition_list){
			HashMap<String, Double> action_map = objDef.getActions();
			
			//NEED TO LOOK UP OBJECT DEFINITION IN MEMORY, IF IT EXISTS, THEN IT SHOULD BE LOADED AND USED, 
			//IF NOT THEN IT SHOULD BE CREATED, POPULATED, AND SAVED
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

			/*
			if(action_map.containsKey(last_action)){
				System.out.println("Last action : "+last_action + " exists in action_map for object");
				last_reward = action_map.get(last_action);
			}
			*/
			System.err.println("last reward : "+last_reward);
			System.err.println("actual_reward : "+actual_reward);
			System.err.println("estimated_reward : "+estimated_reward);
			
			double q_learn_val = q_learn.calculate(last_reward, actual_reward, estimated_reward );
			action_map.put(last_action, q_learn_val);
			System.err.println(" -> ADDED LAST ACTION TO ACTION MAP :: "+last_action+"...Q LEARN VAL : "+q_learn_val);

			objDef.setActions(action_map);
			com.tinkerpop.blueprints.Vertex v = objDef.findAndUpdateOrCreate(persistor);
		}
		
		
		//Learning process
		//	1. identify vocabulary that this set of object info belongs to
		//  2. Create record based on vocabulary
		//  3. load known vocabulary action policies
		//  4. perform matrix math to inline update vocabulary policies for record based on reward/penalty for productivity  
		//  5. 
		//  6. 
	
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
							
		
		
		
		
		
		//MemoryState memState = new MemoryState(last_page.hashCode());
		//com.tinkerpop.blueprints.Vertex state_vertex = null;
		//try{
		//	state_vertex = memState.createAndLoadState(last_page, null, persistor);
		//}catch(IllegalArgumentException e){}

		//get all objects for the chosen page_element	
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
	public static void learn(Path path,
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
		
		PathObject prev_obj = null;
		for(PathObject obj : path.getPath()){
			List<ObjectDefinition> decomposer = DataDecomposer.decompose(obj.data());

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
		
		
		
		
		/*
		
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
		*/
	}
	
	private Vocabulary predictVocabulary(List<ObjectDefinition> object_list){
		return null;
	}
	
	private List<ObjectDefinition> generateVocabRecord(List<ObjectDefinition> object_list, Vocabulary vocabulary){
		
	}
	
	private List<List<ObjectDefinition>> loadActionPolicies(List<ObjectDefinition> object_list, Vocabulary vocabulary){
		
	}
	
	private void backPropogate(){
		
	}
	
	private void saveActionPolicies(){
		
	}
	
	private 
	@Deprecated
	public static synchronized void registerPath(Path path){
		
		
		/**
		 * THIS NEXT BLOCK IS A WORK IN PROGRESS THAT WILL NOT BE ADDED UNTIL AFTER THE PROTOTYPE IS COMPLETE
		 * 
		 * THIS NEEDS TO BE MOVED TO A DIFFERENT ACTOR FOR MACHINE LEARNING. THIS METHOD IS NOT LONGER USED
		 * 
		 * !!!!!!!!!!!!!!!     DO NOT DELETE           !!!
		 * 
		 */
		/*
		for(PathObject<?> pathObj : path.getPath()){
		// generate vocabulary matrix using pathObject
			//decompose path obj
			DataDecomposer decomposer = new DataDecomposer();
			
			List<ObjectDefinition> objDefinitionList = null;
			try {
				objDefinitionList = decomposer.decompose(pathObj.getData());
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Load list for vocabublary, initializing all entries to 0
			ArrayList<Boolean> vocabularyExperienceRecord = new ArrayList<Boolean>(Arrays.asList(new Boolean[this.vocab.getValueList().size()]));
			Collections.fill(vocabularyExperienceRecord, Boolean.FALSE);
			
			//Find last action
			String last_action = "";

			//Get last action experienced in path
			for(int idx = path.getPath().size()-1; idx >= 0 ; idx--){
				if(path.getPath().get(idx).getData() instanceof Action){
					last_action = ((Action)path.getPath().get(idx).getData()).getName();
					break;
				}
			}
			
			//load vocabulary weights
			VocabularyWeights vocab_weights = VocabularyWeights.load("html_actions");
			
			//Run through object definitions and make sure that they are all included in the vocabulary
			Random rand = new Random();
			for(ObjectDefinition objDef : objDefinitionList){
				if( !this.vocab.getValueList().contains(objDef.getValue())){
					//then add object value to end of vocabulary
					this.vocab.appendToVocabulary(objDef.getValue());
					
					//add a new 0.0 value weight to end of weights
<<<<<<< HEAD
					//this.vocab.appendToWeights(objDef.getValue(), rand.nextFloat());
					
					//add new actions entry to match vocab
					ArrayList<Float> action_weights = new ArrayList<Float>(Arrays.asList(new Float[ActionFactory.getActions().length]));
					
					
					//for(int weight_idx = 0 ; weight_idx < action_weights.size(); weight_idx++){
						//System.out.println("SETTING ACTION WIGHT : "+rand.nextFloat());
					//	action_weights.set(weight_idx, rand.nextFloat());
					//}
=======
					vocab_weights.appendToVocabulary(objDef.getValue());

>>>>>>> f8550e37a7b03a9e5d435acb6d8ce040379bea09
					//add weights to vocabulary weights;
					for(String action : ActionFactory.getActions()){
						vocab_weights.appendToWeights(objDef.getValue(), action, rand.nextFloat()); 
					}
					
					//action_weights_list.add(action_weights);
					vocabularyExperienceRecord.add(true);

				}
			}
			

			//PERSIST VOCABULARY
			this.vocab.save();
			vocab_weights.save();
			
			//Create experience record and weight record
			Boolean[][] experience_record = new Boolean[this.vocab.getValueList().size()][ActionFactory.getActions().length];
			double[][] weight_record = new double[this.vocab.getValueList().size()][ActionFactory.getActions().length];
			
			for(ObjectDefinition objDef : objDefinitionList){
				int value_idx = this.vocab.getValueList().indexOf(objDef.getValue() );
				for(int i=0; i < ActionFactory.getActions().length; i++){
					if(ActionFactory.getActions()[i].equals(last_action)){
						vocabularyExperienceRecord.set(value_idx, Boolean.TRUE);
						experience_record[value_idx][i] = Boolean.TRUE;
					}
					else{
						experience_record[value_idx][i] = Boolean.FALSE;
					}
					
					//place weights into 2 dimensional array for NN computations later
					weight_record[value_idx][i] = vocab_weights.getVocabulary_weights().get(objDef.getValue()).get(ActionFactory.getActions()[i]);
				}
			}
			
			// home rolled NN single layer FOLLOWS
			
			//unroll vocabulary weights
			
			
			
			//run vocabularyExperienceRecord with loaded weights through NN
			Sigmoid sigmoid = new Sigmoid();

			
			
			//perform reinforcement learning based on last action taken and result against predictions

			
			// apply reinforcement learning
			// based on result of crawl and predicted values, updated predicted values
			String[] actions = ActionFactory.getActions();
			float[] predictions = new float[actions.length];
			for(int action_index = 0; action_index < predictions.length; action_index++){
				if(actions[action_index].equals(last_action) && isValuable.equals(Boolean.TRUE)){
					predictions[action_index] += 1;
				}
				else if(actions[action_index].equals(last_action) && isValuable.equals(Boolean.FALSE)){
					predictions[action_index] -= 1;
				}
			}
		
			// Backpropagate updated results back through layers
	
		}
		
		 */
		
	}


}
