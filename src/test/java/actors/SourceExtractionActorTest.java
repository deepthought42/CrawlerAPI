package actors;

import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;

import akka.actor.ActorSystem;

import org.springframework.beans.factory.annotation.Autowired;

import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;
import com.looksee.models.enums.SubscriptionPlan;
import com.looksee.services.AccountService;
import com.looksee.services.BrowserService;
import com.looksee.services.SubscriptionService;
import com.stripe.exception.StripeException;

//New
import com.looksee.actors.SourceExtractionActor;

@SpringBootTest
public class SourceExtractionActorTest {

	
	@Autowired
	private BrowserService browser_service;

	@Autowired
	private SourceExtractionActor source_actor;
	
	@Before
	public void start(){
		
    }
	
	@Test
	public void pageSourceSource() throws MalformedURLException, URISyntaxException {
		URL sanitized_url = new URL("http://www.wikipedia.org");
		
	}
}