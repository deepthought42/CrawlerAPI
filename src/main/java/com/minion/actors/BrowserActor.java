package com.minion.actors;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.minion.actors.MemoryRegistryActor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.minion.api.PastPathExperienceController;
import com.qanairy.models.Test;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.OrientConnectionFactory;
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
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
public class BrowserActor extends UntypedActor {
	private static Logger log = LogManager.getLogger(BrowserActor.class.getName());

	private static Random rand = new Random();
	private UUID uuid = null;
	private Browser browser = null;

		
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
		/*List<ObjectDefinition> definitions = DataDecomposer.decompose(pageElement);

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
		*/
		return null;
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
		/*
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
		*/
		
		return null;
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
				log.info("Path passed to BrowserActor");
				Path path = (Path)acct_msg.getData();
				
				String browser = "";
				if(acct_msg.getOptions().isEmpty()){
					
				}
			  	this.browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString(), "phantomjs");

				System.err.println("Creating new Browser");
				Page result_page = null;
				
				if(path.getPath() != null){
					result_page = Crawler.crawlPath(path, this.browser);
				}
				
				Page last_page = path.findLastPage();
				last_page.setLandable(last_page.checkIfLandable());
				
				if(last_page.isLandable()){
					//clone path starting at last page in path
					//Path shortened_path = path.clone());
				}
				
				PathRepository path_repo = new PathRepository();
				path.setKey(path_repo.generateKey(path));
				System.err.println("Browser Actor crawled path : "+path_repo.generateKey(path));
				System.err.println("Path length of Path Message : "+path.getPath().size());
				
				System.err.println("Checking equality of page sources " + last_page.equals(result_page));
				if(last_page.equals(result_page) && path.getPath().size() > 1){
					log.debug("Page sources match(Path Message)");
			  		path.setIsUseful(false);
			  	}
			  	else{
			  		log.debug("PAGES ARE DIFFERENT, PATH IS VALUABLE (Path Message)");
			  		System.err.println("PAGES ARE DIFFERENT, PATH IS VALUABLE (Path Message)");
					
			  		path.setIsUseful(true);
					//if(path.size() > 1){
					//	path.add(result_page);
					//}

					final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
					path_expansion_actor.tell(acct_msg, getSelf() );
					
					Test test = new Test(path, result_page, new Domain(result_page.getUrl().getHost()));
					TestRepository test_repo = new TestRepository();
					test.setKey(test_repo.generateKey(test));
					
					// CHECK THAT TEST HAS NOT YET BEEN EXPERIENCED RECENTLY
					SessionTestTracker seqTracker = SessionTestTracker.getInstance();
					TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
					
					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());
					if(testMap != null && !testMap.containsTest(test)){
						final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
						work_allocator.tell(test_msg, getSelf() );
						testMap.addTest(test);
					}

					log.info("Sending test to Memory Actor");
					//tell memory worker of path
					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					
					//tell memory worker of path
					memory_actor.tell(test_msg, getSelf() );
					//broadcast test
					PastPathExperienceController.broadcastTestExperience(test);
			  	}
			  	this.browser.close();
	
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if (acct_msg.getData() instanceof ExploratoryPath){
				ExploratoryPath exploratory_path = (ExploratoryPath)acct_msg.getData();
			  	this.browser = new Browser(((Page)exploratory_path.getPath().get(0)).getUrl().toString(), "phantomjs");

				System.err.println("Creating new Browser for exploratory path crawling");
				Page result_page = null;

				// IF PAGES ARE DIFFERENT THEN DEFINE NEW TEST THAT HAS PATH WITH PAGE
				// 	ELSE DEFINE NEW TEST THAT HAS PATH WITH NULL PAGE
				
				Page last_page = exploratory_path.findLastPage();
				last_page.setLandable(last_page.checkIfLandable());
				if(last_page.isLandable()){
					//clone path starting at last page in path
					//Path shortened_path = path.clone());
				}

				if(exploratory_path.getPath() != null){
					//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
					//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 
					for(Action action : exploratory_path.getPossibleActions()){
						Path crawl_path = new Path(exploratory_path.getPath());

						result_page = Crawler.crawlPath(crawl_path, this.browser, action);
						
						System.err.println("Checking equality of page sources " + last_page.equals(result_page));
						if(last_page.equals(result_page)){
					  		System.err.println("Page sources match, marking not valuable, (Path Message)");
					  		crawl_path.setIsUseful(false);
					  	}
					  	else{
					  		System.err.println("PAGES ARE DIFFERENT, PATH IS VALUABLE (Path Message)");

					  		if(ExploratoryPath.hasCycle(exploratory_path, last_page)){
					  			break;
					  		}
					  		crawl_path.add(action);
					  		crawl_path.setIsUseful(true);
							if(crawl_path.size() > 1){
								crawl_path.add(result_page);
							}
							
							PathRepository path_repo = new PathRepository();
							crawl_path.setKey(path_repo.generateKey(crawl_path));
							
							Test test = new Test(crawl_path, result_page, new Domain(result_page.getUrl().getHost()));
							TestRepository test_repo = new TestRepository();
							test.setKey(test_repo.generateKey(test));
							
							
							// CHECK THAT TEST HAS NOT YET BEEN EXPERIENCED RECENTLY
							SessionTestTracker seqTracker = SessionTestTracker.getInstance();
							TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
							
							Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());
							if(!testMap.containsTest(test)){
								final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
								work_allocator.tell(test_msg, getSelf() );
								testMap.addTest(test);
							}

							//tell memory worker of path
							//final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
														
							//tell memory worker of path
							//memory_actor.tell(test_msg, getSelf() );
							//broadcast test
							PastPathExperienceController.broadcastTestExperience(test);
							
							Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), crawl_path);

							final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
							path_expansion_actor.tell(path_msg, getSelf() );
					  	}
					}
				}

			  	this.browser.close();
				
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if(acct_msg.getData() instanceof URL){
				log.debug("URL PASSED TO BROWSER ACTOR : " +((URL)acct_msg.getData()).toString());
			  	Browser browser = new Browser(((URL)acct_msg.getData()).toString(), "phantomjs");
			  	
			  	Path path = new Path();
			  	Page page_obj = browser.getPage();
			  	path.getPath().add(page_obj);
			  	Page current_page = Crawler.crawlPath(path, browser);

				Test test = new Test(path, current_page, new Domain(current_page.getUrl().getHost()));
				TestRepository test_repo = new TestRepository();
				test.setKey(test_repo.generateKey(test));
				
				PathRepository path_repo = new PathRepository();
				path.setKey(path_repo.generateKey(path));
				
				PastPathExperienceController.broadcastTestExperience(test);
				
			  	Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);
				final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistryActor"+UUID.randomUUID());
				memory_actor.tell(test_msg, getSelf() );
				
				Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);
				final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
				path_expansion_actor.tell(path_msg, getSelf() );

				browser.close();
		   }
		}else unhandled(message);
	}
}