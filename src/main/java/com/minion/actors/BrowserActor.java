package com.minion.actors;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.actors.MemoryRegistryActor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.minion.api.PastPathExperienceController;
import com.qanairy.models.Test;
import com.qanairy.persistence.OrientDbPersistor;
import com.qanairy.rl.learning.Brain;
import com.qanairy.rl.memory.DataDecomposer;
import com.qanairy.rl.memory.ObjectDefinition;
import com.qanairy.rl.memory.Vocabulary;
import com.minion.browsing.ActionFactory;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;


import com.minion.structs.Message;
import com.minion.structs.SessionTestTracker;
import com.minion.structs.TestMapper;
import com.qanairy.models.Domain;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
public class BrowserActor extends UntypedActor {
    private static final Logger log = LoggerFactory.getLogger(BrowserActor.class);

	private static Random rand = new Random();
	private UUID uuid = null;
	private Browser browser = null;
	private OrientDbPersistor persistor = new OrientDbPersistor();
		
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
		List<ObjectDefinition> definitions = DataDecomposer.decompose(pageElement);

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
					for(String action: ActionFactory.getActions()){						
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
		if(message instanceof Message){
			log.info("Browser actor received message");
			Message<?> acct_msg = (Message<?>)message;
			if (acct_msg.getData() instanceof Path){
				log.info("PATH PASSED TO BROWSER ACTOR");
				Path path = (Path)acct_msg.getData();
				Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);
			  	this.browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString(), "headless");

				log.info("Creating new Browser");
				Page result_page = null;
				
				if(path.getPath() != null){
					log.info("crawling path");
					result_page = Crawler.crawlPath(path, this.browser);
				}
				
				log.info("Getting last page");
				Page last_page = path.findLastPage();
				last_page.setLandable(last_page.checkIfLandable());
				
				if(last_page.isLandable()){
					//clone path starting at last page in path
					//Path shortened_path = path.clone());
				}
				
				log.info("Checking equality of page sources " + last_page.equals(result_page));
				if(last_page.equals(result_page)){
			  		log.info("Page sources match(Path Message)");
			  		path.setIsUseful(false);
			  	}
			  	else{
			  		log.info("PAGES ARE DIFFERENT, PATH IS VALUABLE (Path Message)");
					path.setIsUseful(true);
					if(path.size() > 1){
						path.add(result_page);
					}

					final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
					path_expansion_actor.tell(path_msg, getSelf() );
					
					Test new_test = new Test(path, result_page, new Domain(result_page.getUrl().getHost(), new Organization("Qanairy")));
					// CHECK THAT TEST HAS NOT YET BEEN EXPERIENCED RECENTLY
					SessionTestTracker seqTracker = SessionTestTracker.getInstance();
					TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
					if(!testMap.containsTest(new_test)){
						Message<Test> new_test_msg = new Message<Test>(acct_msg.getAccountKey(), new_test);
						final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
						work_allocator.tell(new_test_msg, getSelf() );
						testMap.addTest(new_test);
					}

					log.info("Sending test to Memory Actor");
					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), new_test);
					//tell memory worker of path
					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					
					//tell memory worker of path
					memory_actor.tell(test_msg, getSelf() );
					//broadcast test
					PastPathExperienceController.broadcastTestExperience(new_test);
			  	}
			  	this.browser.close();
	
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if (acct_msg.getData() instanceof ExploratoryPath){
				log.info("EXPLORATORY PATH PASSED TO BROWSER ACTOR");
				ExploratoryPath path = (ExploratoryPath)acct_msg.getData();
			  	this.browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString(), "headless");

				log.info("Creating new Browser");
				Page result_page = null;

				// IF PAGES ARE DIFFERENT THEN DEFINE NEW TEST THAT HAS PATH WITH PAGE
				// 	ELSE DEFINE NEW TEST THAT HAS PATH WITH NULL PAGE
				log.info("Sending test to Memory Actor");
				Test test = new Test(path, result_page, new Domain(result_page.getUrl().getHost()));
				Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);
				
				log.info("Getting last page");
				Page last_page = path.findLastPage();
				last_page.setLandable(last_page.checkIfLandable());
				if(last_page.isLandable()){
					//clone path starting at last page in path
					//Path shortened_path = path.clone());
				}

				
				if(path.getPath() != null){
					log.info("crawling exploratory path");
					for(Action action : path.getPossibleActions()){
						Path crawl_path  = new Path(path.getPath());

						result_page = Crawler.crawlPath(crawl_path, this.browser, action);
						
						log.info("Checking equality of page sources " + last_page.equals(result_page));
						if(last_page.equals(result_page)){
					  		log.info("Page sources match(Path Message)");
					  		crawl_path.setIsUseful(false);
					  	}
					  	else{
					  		crawl_path.add(action);
					  		log.info("PAGES ARE DIFFERENT, PATH IS VALUABLE (Path Message)");
					  		crawl_path.setIsUseful(true);
							if(crawl_path.size() > 1){
								crawl_path.add(result_page);
							}
							Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), crawl_path);

							final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
							path_expansion_actor.tell(path_msg, getSelf() );
							
							Test new_test = new Test(crawl_path, result_page, new Domain(result_page.getUrl().getHost(), new Organization("Qanairy")));
							// CHECK THAT TEST HAS NOT YET BEEN EXPERIENCED RECENTLY
							SessionTestTracker seqTracker = SessionTestTracker.getInstance();
							TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
							if(!testMap.containsTest(new_test)){
								Message<Test> new_test_msg = new Message<Test>(acct_msg.getAccountKey(), new_test);
								final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
								work_allocator.tell(new_test_msg, getSelf() );
								testMap.addTest(new_test);
							}

							log.info("Sending test to Memory Actor");
							Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), new_test);
							//tell memory worker of path
							final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
							
							//tell memory worker of path
							memory_actor.tell(test_msg, getSelf() );
							//broadcast test
							PastPathExperienceController.broadcastTestExperience(new_test);
							
							break;
					  	}
					}
				}

			  	this.browser.close();
	
				
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if(acct_msg.getData() instanceof URL){
				log.info("URL PASSED TO BROWSER ACTOR : " +((URL)acct_msg.getData()).toString());
			  	Browser browser = new Browser(((URL)acct_msg.getData()).toString(), "headless");
			  	
			  	log.info("creating path");
			  	Path path = new Path();
			  	
			  	log.info("getting browser page");
			  	Page page_obj = browser.getPage();
			  	
			  	log.info("adding page " + page_obj + " to path");
			  	path.getPath().add(page_obj);
			  	
			  	log.info("Crawling path");
			  	Page current_page = Crawler.crawlPath(path, browser);
			  	
			  	log.info("Getting last and current page");
			  	Page last_page = path.findLastPage();
			  	
			  	if(last_page != null && last_page.getSrc().equals(Browser.cleanSrc(current_page.getSrc())) && path.getPath().size() > 1){
			  		log.info("Path isn't useful");
			  		path.setIsUseful(false);
			  	}
			  	else{
			  		log.info("Path is useful");

					path.setIsUseful(true);

					System.out.println("PATH LENGTH : "+path.getPath().size());
					Test test = new Test(path, current_page, new Domain(current_page.getUrl().getHost()));
					PastPathExperienceController.broadcastTestExperience(test);
					
					log.info("Wrapping test in Message");
				  	Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);

					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistryActor"+UUID.randomUUID());
					log.info("Sending test message to memory actor");
					memory_actor.tell(test_msg, getSelf() );
					
					log.info("Wrapping path in Message");

					Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);
					final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
					path_expansion_actor.tell(path_msg, getSelf() );
			  	}
			  	browser.close();

				Test test = new Test(path, current_page, new Domain(current_page.getUrl().getHost()));
				PastPathExperienceController.broadcastTestExperience(test);
				
				log.info("Saving test");
			  	Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);

				final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistryActor"+UUID.randomUUID());
				memory_actor.tell(test_msg, getSelf() );
		   }
		}else unhandled(message);
	}
}