package com.minion.actors;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;

import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.TestDao;
import com.qanairy.models.dao.impl.DomainDaoImpl;
import com.qanairy.models.dao.impl.TestDaoImpl;
import com.qanairy.persistence.Domain;
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
			Message<?> msg = (Message<?>)message;
			//Retains lists of productive, unproductive, and unknown value {@link Path}s.
			if(msg.getData() instanceof Test){
				Test test = (Test)msg.getData();
				TestDao test_repo = new TestDaoImpl();
				test = test_repo.save(test);
				DomainDao domain_dao = new DomainDaoImpl();
				
				String host_url = msg.getOptions().get("host").toString();
				Domain domain = domain_dao.find(host_url);
				boolean test_already_exists = false;
				for(Test test_record : domain.getTests()){
					if(test_record.getKey().equals(test.getKey())){
						test_already_exists = true;
					}
				}
				if(!test_already_exists){
					domain.addTest(test);
				}
				domain.setTestCount(domain.getTestCount()+1);

				
				if(test.getBrowserStatuses() == null || test.getBrowserStatuses().isEmpty()){
					MessageBroadcaster.broadcastDiscoveredTest(test, host_url);
				}
				else{
					MessageBroadcaster.broadcastTest(test, host_url);
				}
			}
		}
		else unhandled(message);
	}
}
