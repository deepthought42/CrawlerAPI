package com.minion.actors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.minion.actors.MemoryRegistryActor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.qanairy.models.Test;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.IDomain;
import com.qanairy.rl.memory.Vocabulary;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;


import com.minion.structs.Message;
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Group;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
public class BrowserActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(BrowserActor.class.getName());

	private static Random rand = new Random();
	private UUID uuid = null;

		
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

		log.info(getSelf().hashCode() + " -> GETTING BEST ACTION PROBABILITY...");
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
			log.info("Browser actor received message");
			Message<?> acct_msg = (Message<?>)message;

			Browser browser = null;
			if (acct_msg.getData() instanceof ExploratoryPath){
				ExploratoryPath exploratory_path = (ExploratoryPath)acct_msg.getData();
				log.info("exploratory path started");
				browser = new Browser(((Page)exploratory_path.getPath().get(0)).getUrl().toString(), "phantomjs");
				
				Page last_page = exploratory_path.findLastPage();
				last_page.setLandable(last_page.checkIfLandable());

				if(last_page.isLandable()){
					//clone path starting at last page in path
					//Path shortened_path = path.clone());
				}

				
				if(exploratory_path.getPath() != null){
					Page result_page = null;

					// increment total paths being explored for domain
					String domain_url = last_page.getUrl().getHost();
					DomainRepository domain_repo = new DomainRepository();
					IDomain idomain = domain_repo.find(domain_url);
					idomain.setLastDiscoveryPathRanAt(new Date());
					
					//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
					//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 
					for(Action action : exploratory_path.getPossibleActions()){
						Path path = Path.clone(exploratory_path);
						path.add(action);
						log.info("Crawling exploratory path with length : " + path.size());
						final long pathCrawlStartTime = System.currentTimeMillis();
						result_page = Crawler.crawlPath(path, browser);
						final long pathCrawlEndTime = System.currentTimeMillis();

						long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
						
						int last_idx = exploratory_path.getPath().size()-1;
						if(last_idx < 0){
							last_idx = 0;
						}
						
						int clicks = getLastClicksSequenceCount(last_idx, exploratory_path, last_page);
						if(clicks >= 3 && last_page.equals(result_page)){
							//check if test has 3 or more consecutive click events since last page
					  		path.setIsUseful(false);
					  	}
					  	else{
					  		if(ExploratoryPath.hasCycle(path, last_page)){
					  			log.info("exploratory path has cycle; exiting");
					  			break;
					  		}

					  		createTest(path, result_page, pathCrawlRunTime, acct_msg);
					  	}
					}
				}

			  	browser.close();
				
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if (acct_msg.getData() instanceof Path){
				log.info("Path started");

				Path path = (Path)acct_msg.getData();
				assert(path.getPath() != null);
				if(acct_msg.getOptions().isEmpty()){
				}
				
				browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString(), "phantomjs");
				
				Page result_page = null;
				long crawl_time_in_ms = -1L;
				final long pathCrawlStartTime = System.currentTimeMillis();
				result_page = Crawler.crawlPath(path, browser);	
				final long pathCrawlEndTime = System.currentTimeMillis();
				
				crawl_time_in_ms = pathCrawlEndTime - pathCrawlStartTime;
				
				Page last_page = path.findLastPage();

				last_page.setLandable(last_page.checkIfLandable());
				if(last_page.isLandable()){
					//clone path starting at last page in path
					//Path shortened_path = path.clone());
				}
				
				PathRepository path_repo = new PathRepository();
				path.setKey(path_repo.generateKey(path));
				int last_idx = path.getPath().size()-1;
				if(last_idx < 0){
					last_idx = 0;
				}
				int clicks = getLastClicksSequenceCount(last_idx, path, last_page);
				if(clicks >= 3 && last_page.equals(result_page) && path.getPath().size() > 1){
			  		path.setIsUseful(false);
			  	}
			  	else{					
			  		createTest(path, result_page, crawl_time_in_ms, acct_msg);
			  	}
			  	browser.close();
	
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if(acct_msg.getData() instanceof URL){
				log.info("Url provided");

				try{
					browser = new Browser(((URL)acct_msg.getData()).toString(), "phantomjs");
				}
				catch(NullPointerException e){
					log.error("Failed to open connection to browser");
					return;
				}
				generate_landing_page_test(browser, acct_msg);

				browser.close();
		   }
			//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);

		}else unhandled(message);
	}
	
	/**
	 * Generates {@link Test Tests} for path
	 * @param path
	 * @param result_page
	 */
	private void createTest(Path path, Page result_page, long crawl_time, Message<?> acct_msg ) {
		path.setIsUseful(true);
		log.info("usefulness set on path");
		Test test = new Test(path, result_page, new Domain(result_page.getUrl().getHost(), result_page.getUrl().getProtocol()));							
		TestRepository test_repo = new TestRepository();
		test.setKey(test_repo.generateKey(test));
		test.setRunTime(crawl_time);
		addFormGroupsToPath(test);
		log.info("sending test message out");
		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());
		
		//tell memory worker of test
		final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
		memory_actor.tell(test_msg, getSelf() );
		
		Path new_path = Path.clone(path);
		new_path.add(result_page);
		Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), new_path, acct_msg.getOptions());

		final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
		path_expansion_actor.tell(path_msg, getSelf() );
	}

	/**
	 * Adds Group labeled "form" to test if the test has any elements in it that have form in the xpath
	 * 
	 * @param test {@linkplain Test} that you want to label
	 */
	private void addFormGroupsToPath(Test test) {
		//check if test has any form elements
		for(PathObject path_obj: test.getPath().getPath()){
			if(path_obj.getClass().equals(PageElement.class)){
				PageElement elem = (PageElement)path_obj;
				if(elem.getXpath().contains("form")){
					test.addGroup(new Group("form"));
					break;
				}
			}
		}
	}

	/**
	 * counts how many clicks have happened in a sequence since last page change
	 * 
	 * @param last_idx
	 * @param path
	 * @param last_page
	 * @return
	 */
	private int getLastClicksSequenceCount(int last_idx, Path path, Page last_page) {
		int clicks = 0;
		
		while(last_idx>=0){
			if(path.getPath().get(last_idx).equals(last_page)){
				break;
			}
			PathObject obj = path.getPath().get(last_idx);
			if(obj.getType().equals("Action")){
				log.info("checking action in exploratory path");
				Action path_action = (Action)obj;
				if(path_action.getName().equals("click") || path_action.getName().equals("doubleclick")){
					log.info("incrementing click count");
					clicks++;
				}
			}
			last_idx--;
		};
		
		return clicks;
	}

	/**
	 * Generates a landing page test based on a given URL
	 * 
	 * @param browser
	 * @param msg
	 * 
	 * @throws MalformedURLException
	 * @throws IOException
	 * 
	 * @pre browser != null
	 * @pre msg != null
	 */
	public void generate_landing_page_test(Browser browser, Message<?> msg) throws MalformedURLException, IOException{
		assert browser != null;
		assert msg != null;
		
		log.info("Generting landing page");
	  	Path path = new Path();
	  	Page page_obj = browser.getPage();
	  	path.getPath().add(page_obj);
	  	//Page current_page = Crawler.crawlPath(path, browser);

		PathRepository path_repo = new PathRepository();
		path.setKey(path_repo.generateKey(path));
		
		Test test = new Test(path, page_obj, new Domain(page_obj.getUrl().getHost(), page_obj.getUrl().getProtocol()));
		TestRepository test_repo = new TestRepository();
		test.setKey(test_repo.generateKey(test));
						
	  	Message<Test> test_msg = new Message<Test>(msg.getAccountKey(), test);
	  	ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistryActor"+UUID.randomUUID());
		memory_actor.tell(test_msg, getSelf() );
		
		Message<Path> path_msg = new Message<Path>(msg.getAccountKey(), path);
		final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
		path_expansion_actor.tell(path_msg, getSelf() );
	}
}