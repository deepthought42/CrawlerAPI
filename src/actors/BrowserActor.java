package actors;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;

import memory.DataDecomposer;
import memory.OrientDbPersistor;
import memory.Vocabulary;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import api.PastPathExperienceController;
import browsing.Browser;
import browsing.Page;
import browsing.PageElement;
import browsing.PathObject;
import structs.Message;
import structs.Path;
import structs.SessionTestTracker;
import structs.TestMapper;
import tester.Test;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 * 
 * @author Brandon Kindred
 *
 */
public class BrowserActor extends UntypedActor {
    private static final Logger log = Logger.getLogger(BrowserActor.class);

	private static Random rand = new Random();
	private UUID uuid = null;
	private Browser browser = null;
	private OrientDbPersistor persistor = new OrientDbPersistor();
	//private ArrayList<Vocabulary> vocabularies = null;
	
	//temporary list for vocab labels into it can be determined how best to handle them
		private String[] vocabLabels = {"html"};
	//SHOULD BE CHANGED!!!
	
		
	/**
	 * Gets a random number between 0 and size
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
		List<Object> definitions = DataDecomposer.decompose(pageElement);

		System.out.println(getSelf().hashCode() + " -> GETTING BEST ACTION PROBABILITY...");
		HashMap<String, Double> cumulative_action_map = new HashMap<String, Double>();
		
		for(Object obj : definitions){
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
	 * Calculate all estimated element probabilities
	 * 
	 * @param page
	 * @param element_probabilities
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public ArrayList<HashMap<String, Double>> getEstimatedElementProbabilities(ArrayList<PageElement> pageElements) 
			throws IllegalArgumentException, IllegalAccessException
	{
		
		ArrayList<HashMap<String, Double>> element_action_map_list = new ArrayList<HashMap<String, Double>>(0);
				
		for(PageElement elem : pageElements){
			HashMap<String, Double> full_action_map = new HashMap<String, Double>(0);
			//find vertex for given element
			List<Object> raw_object_definitions = DataDecomposer.decompose(elem);
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
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			if (acct_msg.getData() instanceof Path){
				log.info("PATH PASSED TO BROWSER ACTOR");
				Path path = (Path)acct_msg.getData();
				Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);
				
				this.browser = new Browser(((Page)(path.getPath().get(0).data())).getUrl().toString());
				if(!path.getPath().isEmpty()){
					Crawler.crawlPath(path, browser);
				}
				 
				//get current page of browser
				Page current_page = browser.getPage();
				Page last_page = path.getLastPage();
				
				
				if(last_page.checkIfLandable(browser)){
					last_page.setLandable(true);
				}
				
				//INSTEAD OF ADDING PAGE TO PATH, SEND PAGE TRANSITION OBJECT MESSAGE TO SITE MAPPER ACTOR FOR PROCESSING.
				//if(path.getPath().size() > 1){
				//	path.add(current_page);
				//}
				
				// IF PAGES ARE DIFFERENT THEN DEFINE NEW TEST THAT HAS PATH WITH PAGE
				// 	ELSE DEFINE NEW TEST THAT HAS PATH WITH NULL PAGE
				log.info("Saving test");
				Test test = new Test(path, current_page);
				Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);
				
				final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor");
				path_expansion_actor.tell(test_msg, getSelf() );

				//add test to sequences for session
				SessionTestTracker seqTracker = SessionTestTracker.getInstance();
				TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
				testMap.addTest(test);
				
				//tell memory worker of path
				final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
				
				//tell memory worker of path
				log.info("Saving test");
				memory_actor.tell(test_msg, getSelf() );
				memory_actor.tell(path_msg, getSelf() );

				//broadcast path
				PastPathExperienceController.broadcastTestExperience(test);
	        	
	        	this.browser.close();
			}
			else if(acct_msg.getData() instanceof URL){
				log.info("URL PASSED TO BROWSER ACTOR : " +((URL)acct_msg.getData()).toString());
			  	this.browser = new Browser(((URL)acct_msg.getData()).toString());
			  	Path path = new Path();
			  	PathObject page_obj = browser.getPage();
			  	path.add(page_obj);
			  	Crawler.crawlPath(path, browser);
			  	
			  	Page current_page = browser.getPage();
				Page last_page = path.getLastPage();
				
			  	if(!current_page.equals(last_page) || path.getPath().size() == 1){
			  		log.info("PAGES ARE DIFFERENT, PATH IS VALUABLE");
					path.setIsUseful(true);
					if(path.getPath().size() > 1){
						path.add(current_page);
					}
					Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);

					final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor");
					path_expansion_actor.tell(path_msg, getSelf() );
				}
				else{
					path.setIsUseful(false);
				}
			  	
			  	//broadcast path
				PastPathExperienceController.broadcastPathExperience(path);		  	
				
				//final ActorRef memory_actor = this.getContext().actorOf(Props.create(ShortTermMemoryHandler.class), "ShortTermMemoryActor");
				//memory_actor.tell(path, getSelf() );
			  	this.browser.close();
		   }
		}else unhandled(message);
	}
}