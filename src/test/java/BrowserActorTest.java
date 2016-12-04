import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.minion.actors.BrowserActor;
import com.minion.structs.Message;
import com.minion.structs.Path;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * 
 * 
 */
public class BrowserActorTest {
	@Test
	public void verifyCleanSrc(){
        ActorSystem system = ActorSystem.create("BrowserActorTest");
        ActorRef browser_actor = system.actorOf(Props.create(BrowserActor.class), "BrowserActorTest"+UUID.randomUUID());

        Message<Path> acct_message = new Message<Path>("Acct_key", new Path());
		browser_actor.tell(acct_message, ActorRef.noSender() );
		
	}
}
