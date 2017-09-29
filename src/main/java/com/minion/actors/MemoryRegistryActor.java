package com.minion.actors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import akka.actor.UntypedActor;

import com.qanairy.models.Test;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.OrientConnectionFactory;

import com.minion.structs.Message;
import com.qanairy.models.Path;

/**
 * Handles the saving of records into orientDB
 *
 *
 */
public class MemoryRegistryActor extends UntypedActor{
    private static Logger log = LogManager.getLogger(MemoryRegistryActor.class);


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){

			Message<?> acct_msg = (Message<?>)message;
			//Retains lists of productive, unproductive, and unknown value {@link Path}s.
			if(acct_msg.getData() instanceof Test){
				Test test = (Test)acct_msg.getData();
				if(test.equals(null)){
					log.info("Test object is null");
				}

				//FramedGraphFactory factory = new FramedGraphFactory(); //(1) Factories should be reused for performance and memory conservation.
				OrientConnectionFactory connection = new OrientConnectionFactory();
				System.err.println("saving test : " + test + " with key : "+test.getKey());
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
				Path path = (Path)acct_msg.getData();
				System.err.println("Saving Path : " +path + " : " + path.getKey() +" to memory Registry");
				PathRepository path_repo = new PathRepository();
				path_repo.create(new OrientConnectionFactory(), path);
			}
		}
		else unhandled(message);
	}
}
