package com.qanairy.config;

import org.springframework.context.ApplicationContext;
import akka.actor.Actor;
import akka.actor.IndirectActorProducer;

public class SpringActorProducer implements IndirectActorProducer {
	 
    private ApplicationContext applicationContext;
 
    private String beanActorName;
 
    public SpringActorProducer(ApplicationContext applicationContext, 
      String beanActorName) {
        this.applicationContext = applicationContext;
        this.beanActorName = beanActorName;

    	System.err.println("ApplicationContext INIT ::   "+applicationContext);
    	System.err.println("Bean actor name INIT :: "+beanActorName);
    }
 
    @Override
    public Actor produce() {
        return (Actor) applicationContext.getBean(beanActorName);
    }
 
    @Override
    public Class<? extends Actor> actorClass() {
    	System.err.println("ApplicationContext ::   "+applicationContext);
    	System.err.println("Bean actor name :: "+beanActorName);
        return (Class<? extends Actor>) applicationContext
          .getType(beanActorName);
    }
}