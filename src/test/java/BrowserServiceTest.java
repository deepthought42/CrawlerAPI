import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.minion.api.DomainController;
import com.qanairy.models.Action;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.ScreenshotSet;


public class BrowserServiceTest {

	
	public void verifySetMergeWorks() throws IOException{
		Set<PageState> page_state = new HashSet<PageState>();
		Set<ScreenshotSet> screen = new HashSet<ScreenshotSet>();
		screen.add(new ScreenshotSet("http://qanairy.com", "chrome"));
		Set<PageElement> elems = new HashSet<PageElement>();
		page_state.add(new PageState("html", "url.com", screen, elems));
		Set<Action> actions = new HashSet<Action>();
		actions.add(new Action("Test"));
		
		
		Set<PathObject> path_objects = DomainController.merge(page_state, actions);

	}
}
