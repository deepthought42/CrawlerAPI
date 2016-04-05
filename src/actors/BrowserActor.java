package actors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import memory.DataDecomposer;
import memory.ObjectDefinition;
import memory.OrientDbPersistor;
import memory.Vocabulary;

import akka.actor.UntypedActor;
import browsing.Browser;
import browsing.Page;
import browsing.PageElement;
import browsing.PathObject;
import structs.Path;

/**
 * This threadable class is implemented to handle the interaction with a browser 
 * 
 * @author Brandon Kindred
 *
 */
public class BrowserActor extends UntypedActor {

	private static Random rand = new Random();
	private UUID uuid = null;
	private Browser browser = null;
	private OrientDbPersistor<ObjectDefinition> persistor = new OrientDbPersistor<ObjectDefinition>();
	//private ArrayList<Vocabulary> vocabularies = null;
	
	//temporary list for vocab labels into it can be determined how best to handle them
		private String[] vocabLabels = {"html"};
	//SHOULD BE CHANGED!!!
	
	/**
	 * 
	 * @param size
	 * @return
	 */
	public int getRandomElementIndex(int size){
		return rand.nextInt(size);
	}
	
	/**
	 * Calculates the rewards
	 * 
	 * @param action_rewards
	 * @param pageElement
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public HashMap<String, Double> calculateActionProbabilities(PageElement pageElement) throws IllegalArgumentException, IllegalAccessException{
		List<ObjectDefinition> definitions = DataDecomposer.decompose(pageElement);

		System.out.println(getSelf().hashCode() + " -> GETTING BEST ACTION PROBABILITY...");
		HashMap<String, Double> cumulative_action_map = new HashMap<String, Double>();
		
		for(ObjectDefinition obj : definitions){
			Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = persistor.findVertices(obj);
			Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = memory_vertex_iter.iterator();
			
			while(memory_iterator.hasNext()){
				com.tinkerpop.blueprints.Vertex mem_vertex = memory_iterator.next();
				HashMap<String, Double> action_map = mem_vertex.getProperty("actions");
				double probability = 0.0;
				if(action_map != null){
					for(String action: action_map.keySet()){
						if(cumulative_action_map.containsKey(action)){
							probability += cumulative_action_map.get(action);
						}
						
						cumulative_action_map.put(action, probability);
					}
				}
				else{
					for(String action: pageElement.getActions()){						
						cumulative_action_map.put(action, probability);
					}
				}
			}
		}
		return cumulative_action_map;
	}
	
	/**
	 * 
	 * @param element_probabilities
	 * @return
	 */
	public int getBestIndex(double[] element_probabilities){
		double p = -1.0;
		int idx = 0;
		int best_idx = -1;
		for(double prob : element_probabilities){
			if(prob > p){
				p = prob;
				best_idx = idx;
			}
			idx++;
		}
		System.out.println("BEST PROBABILITY :: " + p + " at index "+best_idx);
		return best_idx;
	}

	/**
	 * 
	 * @param element_probabilities
	 * @return
	 */
	public String getBestAction(HashMap<String, Double> action_probabilities){
		double p = -1.0;
		String best_action = "click";
		for(String action : action_probabilities.keySet()){
			double action_prob = action_probabilities.get(action);
			if(action_prob > p){
				p = action_prob;
				best_action = action;
			}
		}
		System.out.println("BEST PROBABILITY :: " + p);
		return best_action;
	}
	
	/**
	 * Gets the index for the element that has an action probability of state change greater than the rest
	 * 
	 * @param element_probabilities
	 * @return
	 */
	public int getBestIndex(ArrayList<HashMap<String, Double>> element_probabilities){
		double p = -1.0;
		int idx = 0;
		int best_idx = 0;
		
		for(HashMap<String, Double> action_map : element_probabilities){
			for(String action: action_map.keySet()){
				if(action_map.get(action) > p){
					p = action_map.get(action);
					best_idx = idx;
				}
			}
			idx++;
		}
		System.out.println("BEST PROBABILITY :: " + p + " at index "+best_idx);
		return best_idx;
	}
	
	/**
	 * Calculate all estimated element probabilities
	 * 
	 * @param page
	 * @param element_probabilities
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public ArrayList<HashMap<String, Double>> getEstimatedElementProbabilities(ArrayList<PageElement> pageElements) throws IllegalArgumentException, IllegalAccessException{
		ArrayList<HashMap<String, Double>> element_action_map_list = new ArrayList<HashMap<String, Double>>(0);
				
		for(PageElement elem : pageElements){
			HashMap<String, Double> full_action_map = new HashMap<String, Double>(0);
			//find vertex for given element
			List<ObjectDefinition> raw_object_definitions = DataDecomposer.decompose(elem);
			List<com.tinkerpop.blueprints.Vertex> object_definition_list
				= persistor.findAll(raw_object_definitions);
					
			//iterate over set to get all actions for object definition list
			for(com.tinkerpop.blueprints.Vertex v : object_definition_list){
				HashMap<String, Double> action_map = v.getProperty("actions");
				if(action_map != null && !action_map.isEmpty()){
					for(String action : action_map.keySet()){
						if(!full_action_map.containsKey(action)){
							//If it doesn't yet exist, then seed it with a random variable
							full_action_map.put(action, rand.nextDouble());
						}
						else{
							
							double action_sum = full_action_map.get(action) + action_map.get(action);
							full_action_map.put(action, action_sum);
						}
					}
				}
			}
			
			for(String action : full_action_map.keySet()){
				double probability = 0.0;
				probability = full_action_map.get(action)/(double)object_definition_list.size();

				//cumulative_probability[action_idx] += probability;
				full_action_map.put(action, probability);
			}
			element_action_map_list.add(full_action_map);
		}
		
		return element_action_map_list;
	}
	
	/**
	 * Get the UUID for this Agent
	 */
	public UUID getActorId(){
		return uuid;
	}
	
	/**
	 * Retrieves all {@linkplain Vocabulary vocabularies} that are required by the agent 
	 * 
	 * @param vocabLabels
	 * @return
	 */
	public ArrayList<Vocabulary> loadVocabularies(String[] vocabLabels){
		ArrayList<Vocabulary> vocabularies = new ArrayList<Vocabulary>();
		for(String label : vocabLabels){			
			vocabularies.add(Vocabulary.load(label));
		}
		return vocabularies;		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		 if (message instanceof Path){
			 
			 Path path = Path.clone((Path)message);
			 if(!path.getPath().isEmpty()){
				 Crawler.crawlPath(path, browser);
			 }
			 else{
				 
			 }
			 
			 //determine if path was successful
			 path.setIsUseful(true);
             
             Page current_page = null;
     		
        	 current_page = browser.getPage();

        	 //tell memory worker of path
        	 
     		 this.browser.getDriver().quit();
             
		  }
		  else if(message instanceof URL){
			  this.browser = new Browser(((URL)message).toString());
			  Path path = new Path();
			  PathObject<?> page_obj = new PathObject<Page>(browser.getPage());
			  path.add(page_obj);
			  Crawler.crawlPath(path, browser);
		  }

		  else unhandled(message);
	}
}