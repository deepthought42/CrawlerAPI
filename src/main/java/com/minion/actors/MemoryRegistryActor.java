package com.minion.actors;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;

import com.qanairy.models.Test;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.OrientConnectionFactory;
import com.minion.api.MessageBroadcaster;
import com.minion.structs.Message;
import com.qanairy.models.Path;

/**
 * Handles the saving of records into orientDB
 *
 *
 */
public class MemoryRegistryActor extends UntypedActor{
    private static Logger log = LoggerFactory.getLogger(MemoryRegistryActor.class);


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
				TestRepository test_repo = new TestRepository();
				test.setKey(test_repo.generateKey(test));
				test_repo.save(connection, test);
				if(test.getBrowserPassingStatuses().isEmpty()){
					MessageBroadcaster.broadcastDiscoveredTest(test);
				}
				else{
					MessageBroadcaster.broadcastTest(test);
				}
			}
			else if(acct_msg.getData() instanceof Path){
				Path path = (Path)acct_msg.getData();
				
				PathRepository path_repo = new PathRepository();
				Path path_record = path_repo.find(connection, path_repo.generateKey(path));
				if(path_record == null){
					path_repo.create(connection, path);
				}
			}
			connection.close();
		}
		else unhandled(message);
	}
}
