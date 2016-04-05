package actors;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import structs.Path;

public class TestingBrowserActor extends Thread implements Actor, Callable<Boolean> {
	public final UUID uuid;
	public final Path path;
	public final boolean isExpectingPass;
	
	public TestingBrowserActor(Path path, boolean isExpectingPass){
		uuid = UUID.randomUUID();
		this.path = path;
		this.isExpectingPass = isExpectingPass;
		
		
	}
	
	
	public UUID getActorId() {
		return this.uuid;
	}


	public Boolean call() throws Exception {
		ExecutorService es = Executors.newSingleThreadExecutor();
		
		//Start browser actor
		es.submit(new BrowserActor());

		return null;
	}

}
