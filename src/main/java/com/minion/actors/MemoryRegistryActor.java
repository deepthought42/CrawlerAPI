package com.minion.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.UntypedActor;
import com.minion.api.MessageBroadcaster;
import com.minion.structs.Message;
import com.qanairy.models.Domain;
import com.qanairy.models.Test;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.TestRepository;

/**
 * Handles the saving of records into orientDB
 *
 *
 */
@Component
@Scope("prototype")
public class MemoryRegistryActor extends UntypedActor{
    @SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MemoryRegistryActor.class);
    
    @Autowired
    private TestRepository test_repo;
    
    @Autowired
    private DomainRepository domain_repo;
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.err.println("Memory registry actor recieved a message");
		if(message instanceof Message){
			Message<?> msg = (Message<?>)message;
			//Retains lists of productive, unproductive, and unknown value {@link Path}s.
			if(msg.getData() instanceof Test){
				System.err.println("Test message received by memory registry actor");
				Test test = (Test)msg.getData();
				
				String host_url = msg.getOptions().get("host").toString();
				Test record = test_repo.findByKey(test.getKey());
				
				if(record == null){
					System.err.println("Test REPO :: "+test_repo);
					System.err.println("Test ::  "+test);
					test.setName("Test #" + (domain_repo.getTestCount(host_url)+1));
					Domain domain = domain_repo.findByHost(host_url);
					domain.addTest(test);
					domain_repo.save(domain);
					
					if(test.getBrowserStatuses() == null || test.getBrowserStatuses().isEmpty()){
						MessageBroadcaster.broadcastDiscoveredTest(test, host_url);
					}
					else {
						MessageBroadcaster.broadcastTest(test, host_url);
					}
				}
				else{
					test = record;
				}
			}
		}
		else unhandled(message);
	}
}
