package actors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.looksee.actors.PathExpansionActorOLD;

public class PathExpansionActorTest {

	@Test
	public void verifyInternalLinkCheck() {
		String internal_link = "staging-app.qanairy.com/#steps";
		String noninternal_link = "staging-app.qanairy.com/";
		
		assertTrue(PathExpansionActorOLD.isInternalLink(internal_link));
		assertFalse(PathExpansionActorOLD.isInternalLink(noninternal_link));
	}
}
