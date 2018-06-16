package com.minion.actors;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorSystem;
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
    private ActorSystem actor_system;
    
    @Autowired
    private TestRepository test_repo;
    
    @Autowired
    private DomainRepository domain_repo;
    
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
				test = test_repo.save(test);
				
				String host_url = msg.getOptions().get("host").toString();
				Domain domain = domain_repo.findByHost(host_url);
				boolean test_already_exists = false;
				for(Test test_record : domain.getTests()){
					if(test_record.getKey().equals(test.getKey())){
						test_already_exists = true;
					}
				}
				if(!test_already_exists){
					domain.addTest(test);
				}
				domain_repo.save(domain);
				
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
