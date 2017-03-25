package com.minion.actors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;

import com.qanairy.models.Test;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.rl.memory.DataDecomposer;
import com.qanairy.rl.memory.ObjectDefinition;

import com.minion.structs.Message;
import com.qanairy.models.Page;
import com.qanairy.models.Path;

/**
 * Handles the saving of records into orientDB
 * 
 *
 */
public class MemoryRegistryActor extends UntypedActor{
    private static final Logger log = LoggerFactory.getLogger(MemoryRegistryActor.class);

    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			log.info("Initiating connection to orientdb...");
			/*OrientConnectionFactory connection_factory = new OrientConnectionFactory();
			
			log.info("creating FramedGraphFactory instance");
			FramedGraphFactory factory = new FramedGraphFactory(); //(1) Factories should be reused for performance and memory conservation.
			
			log.info("opening connection to orientdb");
			OrientGraphFactory graphFactory = new OrientGraphFactory("remote:localhost:2480/Thoth", "brandon", "password");

            log.info("Database connection opened");
            */
			Message<?> acct_msg = (Message<?>)message;
			log.info("message converted to message format");
			//Retains lists of productive, unproductive, and unknown value {@link Path}s.
			if(acct_msg.getData() instanceof Page){
				Page page = (Page)acct_msg.getData();
				List<ObjectDefinition> decomposed_list = DataDecomposer.decompose(page);
				//OrientDbPersistor persistor = new OrientDbPersistor();
				
				for(Object objDef : decomposed_list){
					//if object definition value doesn't exist in vocabulary 
					// then add value to vocabulary
					//Vocabulary vocabulary = new Vocabulary(new ArrayList<String>(), "page");
					//vocabulary.appendToVocabulary(((ObjectDefinition)objDef).getValue());
					//persistor.findAndUpdateOrCreate(vocabulary, ActionFactory.getActions());
				}
			}
			else if(acct_msg.getData() instanceof Test){
				log.info("Saving Test to memory Registry");
				Test test = (Test)acct_msg.getData();
				if(test.equals(null)){
					log.info("Test object is null");
				}
				
				//FramedGraphFactory factory = new FramedGraphFactory(); //(1) Factories should be reused for performance and memory conservation.
				OrientConnectionFactory connection = new OrientConnectionFactory();
				log.info("saving test : "+test);
				TestRepository test_repo = new TestRepository();
				
				test_repo.create(connection, test);
				log.info("Commiting changes");

				
				/*
				
				
				//check if test with key already exists.
				OrientDbPersistor persistor = new OrientDbPersistor();
				Iterator<Vertex> vertex_iter = persistor.findVertices(test).iterator();//findByKey(test.getKey());
				// if record already exists then create TestRecord and append it to records for test
				if(vertex_iter.hasNext()){
					log.info("Test already exists....adding test as record");
					Test prev_test = (Test)vertex_iter.next();					
					TestRecord record = new TestRecord(test.getResult(), new Date(), prev_test.getPath().equals(test.getPath()));
					prev_test.addTestRecord(record);
					
				}
				else{
					log.info("Saving test for the first time");
					Date date = new Date();
					TestRecord record = new TestRecord(test.getResult(), date, true);
					log.info("TEST RECORD :: "+record);
					log.info("TEST PATH :: "+test.getPath());
					log.info("DATE :: " + date);
					
					test.addTestRecord(record);
					persistor.findAndUpdateOrCreate(test);
					//persistor.addVertexType(record, OrientDbPersistor.getProperties(record));
					//persistor.createVertex(test);
					//persistor.save();
				}
				*/
			}
			else if(acct_msg.getData() instanceof Path){
				log.info("Converting message to path");
				Path path = (Path)acct_msg.getData();
				log.info("Saving Path : " +path + " : to memory Registry");
				PathRepository path_repo = new PathRepository();
				path_repo.create(new OrientConnectionFactory(), path);
			}
		}
		else unhandled(message);
	}
}
