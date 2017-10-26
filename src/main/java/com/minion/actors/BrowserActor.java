package com.minion.actors;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.qanairy.rl.memory.Vocabulary;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;


import com.minion.structs.Message;
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;

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
	 * 
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Path and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;

			final long browserActorStartTime = System.currentTimeMillis();
			
			if (acct_msg.getData() instanceof ExploratoryPath){
				ExploratoryPath exploratory_path = (ExploratoryPath)acct_msg.getData();
				try{
					this.browser = new Browser(((Page)exploratory_path.getPath().get(0)).getUrl().toString(), "phantomjs");
				}
				catch(NullPointerException e){
					System.out.println("Failed to open connection to browser");
				}
			  	Page result_page = null;

				// IF PAGES ARE DIFFERENT THEN DEFINE NEW TEST THAT HAS PATH WITH PAGE
				// 	ELSE DEFINE NEW TEST THAT HAS PATH WITH NULL PAGE
				
				Page last_page = exploratory_path.findLastPage();
				try{
					last_page.setLandable(last_page.checkIfLandable());
				}catch(Exception e){
					e.printStackTrace();
				}
				if(last_page.isLandable()){
					//clone path starting at last page in path
					//Path shortened_path = path.clone());
				}
				
				if(exploratory_path.getPath() != null){
					//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
					//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 
					for(Action action : exploratory_path.getPossibleActions()){
						Path crawl_path = Path.clone(exploratory_path);
						crawl_path.add(action);

						final long pathCrawlStartTime = System.currentTimeMillis();

						result_page = Crawler.crawlPath(crawl_path, this.browser);
						
						final long pathCrawlEndTime = System.currentTimeMillis();

						long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
						
						System.out.println("Total exploratory path crawl execution time: " + (pathCrawlStartTime - pathCrawlEndTime) );
						if(last_page.equals(result_page)){
					  		System.out.println("exploratory path -> Page sources match, marking not valuable, (Path Message)");
					  		crawl_path.setIsUseful(false);
					  	}
					  	else{
					  		System.out.println("exploratory path -> PAGES ARE DIFFERENT, PATH IS VALUABLE (Path Message)");

					  		if(ExploratoryPath.hasCycle(crawl_path, last_page)){
					  			break;
					  		}

					  		crawl_path.setIsUseful(true);
							
							PathRepository path_repo = new PathRepository();
							crawl_path.setKey(path_repo.generateKey(crawl_path));
							
							Test test = new Test(crawl_path, result_page, new Domain(result_page.getUrl().getHost()));
							test.setRunTime(pathCrawlRunTime);
							
							TestRepository test_repo = new TestRepository();
							test.setKey(test_repo.generateKey(test));
							
							
							Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());
							//final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
							//work_allocator.tell(test_msg, getSelf() );
							//tell memory worker of path
							final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
														
							//tell memory worker of path
							memory_actor.tell(test_msg, getSelf() );
							
							PastPathExperienceController.broadcastTestExperience(test);
							

							//broadcast test
							Path new_crawl_path = Path.clone(crawl_path);
							new_crawl_path.add(result_page);
							Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), new_crawl_path);

							final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
							path_expansion_actor.tell(path_msg, getSelf() );
					  	}
					}
				}

			  	this.browser.close();
				
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if (acct_msg.getData() instanceof Path){
				Path path = (Path)acct_msg.getData();
				
				if(acct_msg.getOptions().isEmpty()){
					
				}
				
				for(PathObject obj : path.getPath()){
					System.err.println("FIRST PATH ELEMENT TYPE ::   ->   " +obj.getType());
				}
				
				try{
					this.browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString(), "phantomjs");
				}
				catch(NullPointerException e){
					System.out.println("Failed to open connection to browser");
				}
				Page result_page = null;
				final long pathCrawlStartTime = System.currentTimeMillis();

				if(path.getPath() != null){
					result_page = Crawler.crawlPath(path, this.browser);	
				}
				
				final long pathCrawlEndTime = System.currentTimeMillis();
				System.out.println("Path crawled in : " + (pathCrawlEndTime - pathCrawlStartTime));
				

				Page last_page = path.findLastPage();
				last_page.setLandable(last_page.checkIfLandable());
				
				if(last_page.isLandable()){
					//clone path starting at last page in path
					//Path shortened_path = path.clone());
				}
				
				PathRepository path_repo = new PathRepository();
				path.setKey(path_repo.generateKey(path));
				
				if(last_page.equals(result_page) && path.getPath().size() > 1){
			  		path.setIsUseful(false);
			  	}
			  	else{					
			  		path.setIsUseful(true);
					Test test = new Test(path, result_page, new Domain(result_page.getUrl().getProtocol()+"://"+result_page.getUrl().getHost()));
					TestRepository test_repo = new TestRepository();
					test.setKey(test_repo.generateKey(test));
					long pathCrawlTime = pathCrawlEndTime - pathCrawlStartTime;
					System.out.println("Path crawl time :: "+pathCrawlTime);
					test.setRunTime(pathCrawlTime);

					// CHECK THAT TEST HAS NOT YET BEEN EXPERIENCED RECENTLY

					Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());

					//final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
					//work_allocator.tell(test_msg, getSelf() );	
					
					//tell memory worker of path
					final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
					
					//tell memory worker of path
					memory_actor.tell(test_msg, getSelf() );
					//broadcast test
					PastPathExperienceController.broadcastTestExperience(test);
					
					Path new_crawl_path = Path.clone(path);
					new_crawl_path.add(result_page);
					Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), new_crawl_path, acct_msg.getOptions());
					
					final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
					path_expansion_actor.tell(path_msg, getSelf() );
			  	}
			  	this.browser.close();
	
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if(acct_msg.getData() instanceof URL){
				Browser browser = null;
				try{
					browser = new Browser(((URL)acct_msg.getData()).toString(), "phantomjs");
				}
				catch(NullPointerException e){
					System.out.println("Failed to open connection to browser");
				}
				
			  	Path path = new Path();
			  	Page page_obj = browser.getPage();
			  	path.getPath().add(page_obj);
			  	//Page current_page = Crawler.crawlPath(path, browser);

				PathRepository path_repo = new PathRepository();
				path.setKey(path_repo.generateKey(path));
				
				Test test = new Test(path, page_obj, new Domain(page_obj.getUrl().toString()));
				TestRepository test_repo = new TestRepository();
				test.setKey(test_repo.generateKey(test));
								
			  	Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);
			  	ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistryActor"+UUID.randomUUID());
				memory_actor.tell(test_msg, getSelf() );
				
				Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);
				final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
				path_expansion_actor.tell(path_msg, getSelf() );
				
				PastPathExperienceController.broadcastTestExperience(test);

				browser.close();
		   }
			final long browserActorEndTime = System.currentTimeMillis();

			long browserActorRunTime = browserActorStartTime - browserActorEndTime;
			System.out.println("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);

		}else unhandled(message);
	}
}