package shortTerm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.analysis.function.Sigmoid;

import memory.DataDecomposer;
import memory.ObjectDefinition;
import memory.PastExperience;
import memory.Vocabulary;
import browsing.ActionFactory;
import browsing.PathObject;
import browsing.actions.Action;
import structs.Path;
import structs.PathRepresentation;

/**
 * Retains lists of productive, unproductive, and unknown value {@link Path}s.
 * 
 * @author Brandon Kindred
 *
 */
public class ShortTermMemoryRegistry {
	private HashMap<String, PathRepresentation> productive_path_hash_queue = null;
	private HashMap<String, PathRepresentation> unproductive_path_hash_queue = null;
	private HashMap<String, PathRepresentation> unknown_outcome_path_hash_queue = null;
	
	private Vocabulary vocab = null;
	private PastExperience past_experience = null;
	
	private HashMap<Integer, PathObject<?>> path_nodes = null;
	
	public ShortTermMemoryRegistry() {
		this.productive_path_hash_queue = new HashMap<String, PathRepresentation>();
		this.unproductive_path_hash_queue = new HashMap<String, PathRepresentation>();
		this.unknown_outcome_path_hash_queue = new HashMap<String, PathRepresentation>();
		this.path_nodes = new HashMap<Integer,PathObject<?>>();
		this.past_experience = new PastExperience();
		
		this.vocab = new Vocabulary("html");
	}
	
	/**
	 * Saves a path to the appropriate hash based on the 
	 * 
	 * @param path
	 * @param isValuable
	 */
	public synchronized void registerPath(Path path, Boolean isValuable){
		
		PathRepresentation path_rep = new PathRepresentation();
		for(PathObject<?> obj : path.getPath()){
			path_rep.addToPath(obj.hashCode());
			this.addNode(obj);
		}
		
		if(isValuable == null){
			registerUnknownOutcomePath(path_rep);
			return;
		}
		else if(isValuable.equals(Boolean.TRUE)){
			registerProductivePath(path_rep);
		}
		else if(isValuable.equals(Boolean.FALSE)){
			registerUnproductivePath(path_rep);
		}
		
		past_experience.appendToPaths(path, isValuable);

		
		/**
		 * THIS NEXT BLOCK IS A WORK IN PROGRESS THAT WILL NOT BE ADDED UNTIL AFTER THE MVP IS COMPLETE
		 * 
		 * 
		 * !!!!!!!!!!!!!!!     DO NOT DELETE           !!!
		 * 
		 */
		/*
		for(PathObject<?> pathObj : path.getPath()){
		// generate vocabulary matrix using pathObject
			//decompose path obj
			DataDecomposer decomposer = new DataDecomposer(pathObj.getData());
			
			List<ObjectDefinition> objDefinitionList = null;
			try {
				objDefinitionList = decomposer.decompose();
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
			
			//List for vocabublary experience record
			ArrayList<Boolean> vocabularyExperienceRecord = new ArrayList<Boolean>(Arrays.asList(new Boolean[this.vocab.getValueList().size()]));
			Collections.fill(vocabularyExperienceRecord, Boolean.FALSE);
			ArrayList<ArrayList<Float>> action_weights_list = new ArrayList<ArrayList<Float>>();
			Random rand = new Random();
			for(ObjectDefinition objDef : objDefinitionList){
				if( !this.vocab.getValueList().contains(objDef.getValue())){
					//then add object value to end of vocabulary
					this.vocab.appendToVocabulary(objDef.getValue());
					
					//add a new 0.0 value weight to end of weights
					this.vocab.appendToWeights(rand.nextFloat());
					
					//add new actions entry to match vocab
					ArrayList<Float> action_weights = new ArrayList<Float>(Arrays.asList(new Float[ActionFactory.getActions().length]));

					for(int weight_idx = 0 ; weight_idx < action_weights.size(); weight_idx++){
						//System.out.println("SETTING ACTION WIGHT : "+rand.nextFloat());
						action_weights.set(weight_idx, rand.nextFloat());
						
					}
					action_weights_list.add(action_weights);
					vocabularyExperienceRecord.add(true);

				}
				else {
					int value_idx = this.vocab.getValueList().indexOf(objDef.getValue());
					vocabularyExperienceRecord.set(value_idx, Boolean.TRUE);
				}
			}
			
			//build vocabularyExperienceRecord
				
			// predict action probabilities
			// home rolled NN single layer
			Sigmoid sigmoid = new Sigmoid();
			String last_action = "";
			if(vocab.getValueList().size() == this.vocab.getWeights().size()){
				//PREDICTION ARRAY FOR ACTIONS
				String[] actions = ActionFactory.getActions();
				//Get last action experienced in path
				for(int idx = path.getPath().size()-1; idx >= 0 ; idx--){
					if(path.getPath().get(idx).getData() instanceof Action){
						last_action = ((Action)path.getPath().get(idx).getData()).getName();
					}
				}
						
				float[] predictions = new float[actions.length];
				for(int action_index = 0; action_index < predictions.length; action_index++){
					for(int i = 0; i < this.vocab.getValueList().size(); i++){
						float product = 0;
						if(vocabularyExperienceRecord.get(i))
						{
							product = this.vocab.getActions().get(i).get(action_index);
						}
						predictions[action_index] += product;
					}
					
					// apply reinforcement learning
					// based on result of crawl and predicted values, updated predicted values
					if(actions[action_index].equals(last_action) && isValuable.equals(Boolean.TRUE)){
						predictions[action_index] += 10;
					}
					else if(actions[action_index].equals(last_action) && isValuable.equals(Boolean.FALSE)){
						predictions[action_index] -= 1;
					}
					
					predictions[action_index] = (float)sigmoid.value(predictions[action_index]);
				}
				System.out.print("Predictions :: ");
				for(float prediction : predictions){
					System.out.print(prediction + ", ");
				}
				System.out.println();
	
			}
			else{
				System.out.println("VOCABULARY LENGTH DOES NOT MATCH WEIGHT LENGTH");
			}
		
			// Backpropagate updated results back through layers
		}
		*/
	}
	
	/**
	 * Used to inform the work allocator that a path was productive and has a positive value
	 * 
	 * @param path
	 */
	private synchronized void registerProductivePath(PathRepresentation path_rep){
		
		
		boolean exists = productive_path_hash_queue.containsKey(path_rep.toString());
		if(!exists){
			productive_path_hash_queue.put(path_rep.toString(), path_rep);
			System.err.println("PRODUCTIVE PATH REGISTERED :: "+path_rep.toString());

		}
	}
	
	/**
	 * Used to inform the work allocator that a path was productive and has a positive value
	 * 
	 * @param path
	 */
	private synchronized void registerUnknownOutcomePath(PathRepresentation path_rep){
		boolean exists = unknown_outcome_path_hash_queue.containsKey(path_rep.toString());
		if(!exists){
			unknown_outcome_path_hash_queue.put(path_rep.toString(), path_rep);
			//System.err.println("UNKNOWN PATH REGISTERED :: "+path_rep.toString());
		}
	}
	
	/**
	 * Used to inform the work allocator that a path was unproductive and has a negative value
	 * 
	 * @param path
	 */
	private synchronized void registerUnproductivePath(PathRepresentation path_rep){
		boolean exists = unproductive_path_hash_queue.containsKey(path_rep.toString());
		if(!exists){
			unproductive_path_hash_queue.put(path_rep.toString(), path_rep);
			System.err.println("UNPRODUCTIVE PATH REGISTERED :: "+path_rep.toString());

		}
	}

	/**
	 * 
	 * @return
	 */
	public HashMap<String, PathRepresentation> getUnknownPaths() {
		return this.unknown_outcome_path_hash_queue;
	}

	/**
	 * 
	 * @return
	 */
	public HashMap<String, PathRepresentation> getProductivePaths(){
		return this.productive_path_hash_queue;
	}
	
	/**
	 * 	
	 * @return
	 */
	public HashMap<String, PathRepresentation> getUnproductivePaths(){
		return this.unproductive_path_hash_queue;
	}
	
	public Vocabulary getVocabulary(){
		return this.vocab;
	}
	
	/**
	 * 	
	 * @return
	 */
	public void addNode(PathObject<?> obj){
		this.path_nodes.put(obj.hashCode(), obj);
	}
	
	/**
	 * 	
	 * @return
	 */
	public HashMap<Integer, PathObject<?>> getPathNodes(){
		return this.path_nodes;
	}
}
