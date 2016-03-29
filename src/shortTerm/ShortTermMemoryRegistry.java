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
import memory.VocabularyWeights;
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
	public final HashMap<String, PathRepresentation> productive_path_hash_queue;
	public final HashMap<String, PathRepresentation> unproductive_path_hash_queue;
	public final HashMap<String, PathRepresentation> unknown_outcome_path_hash_queue;
	
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
			//System.err.println("Registering path with UNKNOWN value");
			return;
		}
		else if(isValuable.equals(Boolean.TRUE)){
			registerProductivePath(path_rep);
			System.err.println("Registering path with PRODUCTIVE value");
		}
		else if(isValuable.equals(Boolean.FALSE)){
			registerUnproductivePath(path_rep);
			System.err.println("Registering path with UNPRODUCTIVE value");
		}
		
		past_experience.appendToPaths(path, isValuable);

		
		/**
		 * THIS NEXT BLOCK IS A WORK IN PROGRESS THAT WILL NOT BE ADDED UNTIL AFTER THE PROTOTYPE IS COMPLETE
		 * 
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
			ArrayList<ArrayList<Float>> action_weights_list = new ArrayList<ArrayList<Float>>();
			
			//Create experience record
			Boolean[][] experienceRecord = new Boolean[this.vocab.getValueList().size()][ActionFactory.getActions().length];
			
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
			VocabularyWeights vocab_weights = new VocabularyWeights("html_actions");
			
			//build vocabularyExperienceRecord
			Random rand = new Random();
			for(ObjectDefinition objDef : objDefinitionList){
				if( !this.vocab.getValueList().contains(objDef.getValue())){
					//then add object value to end of vocabulary
					this.vocab.appendToVocabulary(objDef.getValue());
					
					//add a new 0.0 value weight to end of weights
					//this.vocab.appendToWeights(objDef.getValue(), rand.nextFloat());
					
					//add new actions entry to match vocab
					ArrayList<Float> action_weights = new ArrayList<Float>(Arrays.asList(new Float[ActionFactory.getActions().length]));
					
					
					//for(int weight_idx = 0 ; weight_idx < action_weights.size(); weight_idx++){
						//System.out.println("SETTING ACTION WIGHT : "+rand.nextFloat());
					//	action_weights.set(weight_idx, rand.nextFloat());
					//}
					//add weights to vocabulary weights;
					for(String action : ActionFactory.getActions()){
						vocab_weights.getVocabulary_weights().get(objDef.getValue()).put(action, rand.nextFloat());
					}
					
					//action_weights_list.add(action_weights);
					vocabularyExperienceRecord.add(true);

				}
				else {
					int value_idx = this.vocab.getValueList().indexOf(objDef.getValue() );
					for(int i=0; i < ActionFactory.getActions().length; i++){
						if(ActionFactory.getActions()[i].equals(last_action)){
							vocabularyExperienceRecord.set(value_idx, Boolean.TRUE);
							experienceRecord[value_idx][i] = Boolean.TRUE;
						}
						else{
							experienceRecord[value_idx][i] = Boolean.FALSE;
						}
					}
				}
			}
			
			//PERSIST VOCABULARY
			this.vocab.save();
			
			// home rolled NN single layer FOLLOWS
			
			//Load vocabulary weights from memory
			Vocabulary vocabulary = this.vocab.load("html");
			
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
