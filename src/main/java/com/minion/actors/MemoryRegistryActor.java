package com.minion.actors;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;

import com.qanairy.models.dao.TestDao;
import com.qanairy.models.dao.impl.TestDaoImpl;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.Test;
import com.minion.api.MessageBroadcaster;
import com.minion.structs.Message;

/**
 * Handles the saving of records into orientDB
 *
 *
 */
public class MemoryRegistryActor extends UntypedActor{
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MemoryRegistryActor.class);


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
				TestDao test_repo = new TestDaoImpl();
				test_repo.save(test);
				if(test.getBrowserStatuses().isEmpty()){
					MessageBroadcaster.broadcastDiscoveredTest(test);
				}
				else{
					MessageBroadcaster.broadcastTest(test);
				}
			}
		}
		else unhandled(message);
	}
}
