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
			OrientConnectionFactory connection = new OrientConnectionFactory();
			Message<?> acct_msg = (Message<?>)message;
			//Retains lists of productive, unproductive, and unknown value {@link Path}s.
			if(acct_msg.getData() instanceof Test){
				Test test = (Test)acct_msg.getData();
				if(test.equals(null)){
					log.info("Test object is null");
				}

				//FramedGraphFactory factory = new FramedGraphFactory(); //(1) Factories should be reused for performance and memory conservation.
				System.err.println("saving test : " + test + " with key : "+test.getKey());
				TestRepository test_repo = new TestRepository();

				test_repo.create(connection, test);
				log.info("Commiting changes");
			}
			else if(acct_msg.getData() instanceof Path){
				Path path = (Path)acct_msg.getData();
				System.err.println("Saving Path : " +path + " : " + path.getKey() +" to memory Registry");
				PathRepository path_repo = new PathRepository();
				path_repo.create(connection, path);
			}
			connection.close();
		}
		else unhandled(message);
	}
}
