package shortTerm;

import java.util.HashMap;

import akka.actor.UntypedActor;
import memory.PastExperience;
import memory.Vocabulary;
import browsing.PathObject;
import structs.Path;
import structs.PathRepresentation;

/**
 * Retains lists of productive, unproductive, and unknown value {@link Path}s.
 * 
 * @author Brandon Kindred
 *
 */
public class ShortTermMemoryRegistry extends UntypedActor{
	public static HashMap<String, PathRepresentation> productive_path_hash_queue = new HashMap<String, PathRepresentation>();
	public static HashMap<String, PathRepresentation> unproductive_path_hash_queue = new HashMap<String, PathRepresentation>();
	public static HashMap<String, PathRepresentation> unknown_outcome_path_hash_queue = new HashMap<String, PathRepresentation>();
	
	private Vocabulary vocab = null;
	private static PastExperience past_experience = null;
	
	private static HashMap<Integer, PathObject<?>> path_nodes = null;
	
	/**
	 * Saves a path to the appropriate hash based on the 
	 * 
	 * @param path
	 * @param isValuable
	 */
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
	
	/**
	 * Used to inform the work allocator that a path was productive and has a positive value
	 * 
	 * @param path
	 */
	private static synchronized void registerProductivePath(PathRepresentation path_rep){
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
	private static synchronized void registerUnknownOutcomePath(PathRepresentation path_rep){
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
	private static synchronized void registerUnproductivePath(PathRepresentation path_rep){
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
		return unknown_outcome_path_hash_queue;
	}

	/**
	 * 
	 * @return
	 */
	public HashMap<String, PathRepresentation> getProductivePaths(){
		return productive_path_hash_queue;
	}
	
	/**
	 * 	
	 * @return
	 */
	public HashMap<String, PathRepresentation> getUnproductivePaths(){
		return unproductive_path_hash_queue;
	}
	
	public Vocabulary getVocabulary(){
		return this.vocab;
	}
	
	/**
	 * 	
	 * @return
	 */
	public static void addNode(PathObject<?> obj){
		path_nodes.put(obj.hashCode(), obj);
	}
	
	/**
	 * 	
	 * @return
	 */
	public static HashMap<Integer, PathObject<?>> getPathNodes(){
		return path_nodes;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Path){
			Path path = (Path)message;
			PathRepresentation path_rep = new PathRepresentation();
			for(PathObject<?> pathObj : path.getPath()){
				path_rep.addToPath(pathObj.hashCode());
				addNode(pathObj);
			}
			
			if(path.isUseful() == null){
				registerUnknownOutcomePath(path_rep);
				//System.err.println("Registering path with UNKNOWN value");
				return;
			}
			else if(path.isUseful().equals(Boolean.TRUE)){
				registerProductivePath(path_rep);
				System.err.println("Registering path with PRODUCTIVE value");
			}
			else if(path.isUseful().equals(Boolean.FALSE)){
				registerUnproductivePath(path_rep);
				System.err.println("Registering path with UNPRODUCTIVE value");
			}
			
			past_experience.appendToPaths(path);
		}
		else unhandled(message);
		
	}
}
