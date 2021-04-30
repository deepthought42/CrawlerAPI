package actors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.looksee.actors.PathExpansionActor;

public class PathExpansionActorTest {

	@Test
	public void verifyInternalLinkCheck() {
		String internal_link = "staging-app.qanairy.com/#steps";
		String noninternal_link = "staging-app.qanairy.com/";
		
		assertTrue(PathExpansionActor.isInternalLink(internal_link));
		assertFalse(PathExpansionActor.isInternalLink(noninternal_link));
	}
}
