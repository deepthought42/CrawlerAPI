package services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.minion.api.DomainController;
import com.qanairy.models.Action;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.services.BrowserService;

public class BrowserServiceTest {
	
	public void verifySetMergeWorks() throws IOException{
		Set<PageState> page_state = new HashSet<PageState>();
		List<ElementState> elems = new ArrayList<ElementState>();
		page_state.add(new PageState("url.com", "https://s3-us-west-2.amazonaws.com/qanairy/www.zaelab.com/pagestate::861a2edcfedf97c7ab4040a2420a6b86fe5e2db543880136567daecc6e20e8711f1f8a02586a3eca4a0aa17503ce368560516d456c244f100bd14b0df79ad896006803ca0a01edf30090275bb60e800335163d667c10480416a832009a0050e0805d061d52b010f3561f1744708f6df7d65462cf1386bd2cf2c320c5385576b31c30aa0e6ca5f7b6cc922ad5083aa8b35c5e8a15eaa08c78ca0fece91038015638f76931404c7000c86854a151e0988a2fb6c481c668b83164ba74040a9f13b09282a008d31e6a95313a4853eca2ec142c6222f1c528cd2988b63aa3a8a63ea1558c21fde256736ef5882719c644511842c9999788c389a6e0247031a033a7c67/viewport.png", elems, "", 0, 0, 1288, 844, "chrome"));
		Set<Action> actions = new HashSet<Action>();
		actions.add(new Action("Test"));
		
		
		Set<PathObject> path_objects = DomainController.merge(page_state, actions);
	}
	
	public void screenshotFromUrl() throws MalformedURLException, IOException{
		String checksum = PageState.getFileChecksum(ImageIO.read(new URL("https://s3-us-west-2.amazonaws.com/qanairy/www.terran.us/30550bada37e6c456380737c7dc19abfa22671c20effa861ed57665cf9960e5a/element_screenshot.png")));
	
		System.err.println("Checksum :: " + checksum);
	}
	
	@Test
	public void verifyXpathGenerationWithJsoup(){
		String html = "<html><body><div><a class='test-class'>link1</a><a class='test-class'>link2</a></div><div id='test-id1'></div><div><span></span></div></body></html>";
		
		List<String> visible_elements = BrowserService.getVisibleElementsUsingJSoup(html);
		for(String xpath : visible_elements){
			System.err.println(xpath);
		}
		assert(visible_elements.size() == 4);
		assert(visible_elements.contains("(//div//a[contains(@class,\"test-class\")])[1]"));
		assert(visible_elements.contains("(//div//a[contains(@class,\"test-class\")])[2]"));
		assert(visible_elements.contains("//div[contains(@id,\"test-id1\")]"));
		assert(visible_elements.contains("//div//span"));
	}
}
