package services;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.openqa.selenium.Dimension;

import com.minion.api.DomainController;
import com.minion.browsing.Browser;
import com.qanairy.models.Action;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Template;
import com.qanairy.services.BrowserService;

public class BrowserServiceTest {
	
	@Test
	public void isElementVisibleInPanel(){
		Browser browser = new Browser();
		browser.setXScrollOffset(0);
		browser.setYScrollOffset(0);
		browser.setViewportSize(new Dimension(1224, 844));
		
		ElementState element = new ElementState();
		element.setXLocation(1132);
		element.setYLocation(0);
		element.setWidth(80);
		element.setHeight(56);
		
		boolean is_visible = BrowserService.isElementVisibleInPane(browser, element);
		assertTrue(is_visible);
	}
	
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
		
		List<String> visible_elements = BrowserService.getXpathsUsingJSoup(html);
		for(String xpath : visible_elements){
			System.err.println(xpath);
		}
		assertTrue(visible_elements.size() == 4);
		assertTrue(visible_elements.contains("(//div//a[contains(@class,\"test-class\")])[1]"));
		assertTrue(visible_elements.contains("(//div//a[contains(@class,\"test-class\")])[2]"));
		assertTrue(visible_elements.contains("(//div)[2]"));
		assertTrue(visible_elements.contains("//div//span"));
	}
	
	@Test
	public void verifyListDetectionExtractsAllRepeatedItems(){
		String html = "<html>"
						  +"<body>"
						    +"<div>"
						    +"<div id=\"item1345\" class=\"product__card\">"
						        +"<div id='fhaiuhreoaf120945' class='product_img'>"
						          +"<img src='noImg.jpg' />"
						        +"</div>"
						        +"<div>"
					            	+" fadsf fadsfdsfa fadsfa"
								+"</div>"
						        +"<div class='functions'>"
						          +"<i class='fa fa-pencil'></i>"
						          +"<i  class='fa fa-times'></i>"
						        +"</div>"
						        +"<a href='https://qanairy.com/pricing.html' />"
						      +"</div>"
						      +"<div id='item2' class='product__card'>"
						        +"<div id='fdyairehwafo121422' class='product_img'>"
						          +"<img src='noImg.jpg' />"
						        +"</div>"
						        +"<div>"
					            	+" this is lklj holmk"
								+"</div>"
						        +"<div class='functions'>"
						          +"<i class='fa fa-pencil'></i>"
						          +"<i  class='fa fa-times'></i>"
						        +"</div>"
						        +"<a href='https://qanairy.com/pricing.html' />"
						      +"</div>"
						      +"<div id='item3' class='product__card'>"
						        +"<div id='fdkfdfhaewur1232429335' class='product_img'>"
						          +"<img src='http://qanairy.com/quality/noImg3.jpg' />"
						        +"</div>"
						        +"<div>"
					            	+" this is a testafda"
								+"</div>"
						        +"<div class='functions'>"
						          +"<i class='fa fa-pencil'></i>"
						          +"<i  class='fa fa-times'></i>"
						        +"</div>"
						        +"<a href='https://qanairy.com/pricing.html' />"
						      +"</div>"
						      +"<div id='item4' class='product__card'>"
						        +"<div id='fsaf2313' class='product_img'>"
						          +"<img src='http://qanairy.com/noImg2.jpg' />"
					            +"</div>"
						        +"<div>"
					            	+" fdbfsud a testafda"
								+"</div>"
						        +"<div class='functions'>"
						          +"<i class='fa fa-pencil'></i>"
						          +"<i  class='fa fa-times'></i>"
						        +"</div>"
						        +"<a href='https://qanairy.com/about.html' />"
						      +"</div>"
						      +"<div id='item5' class='product__card'>"
						        +"<div id='fsaf5672313' class='product_img'>"
						          +"<img src='http://qanairy.com/noImg.jpg' />"
					            +"</div>"
					            +"<div>"
					            	+" shut up and get on with it"
								+"</div>"
						        +"<div class='functions'>"
						          +"<i class='fa fa-pencil'></i>"
						          +"<i  class='fa fa-times'></i>"
						        +"</div>"
						        +"<a href='https://qanairy.com/features.html' />"
						      +"</div>"
						      +"<div id='item' class='product__card'>"
						        +"<div class='functions'>"
						          +"<i  class='fa fa-times'></i>"
						        +"</div>"
						      +"</div>"
					        +"</div>"
					      +"</body>"
						+"</html>";

		List<ElementState> element_list = BrowserService.getAllElementsUsingJSoup(html);
		
		BrowserService browser_service = new BrowserService();
		
		
		Map<String, Template> template_elements = browser_service.findTemplates(element_list);
		template_elements = browser_service.reduceTemplatesToParents(template_elements);
		template_elements = browser_service.reduceTemplateElementsToUnique(template_elements);
		
		System.err.println("list elements list size :: "+template_elements.size());
		

		for(Template template : template_elements.values()){
			System.err.println("TEMPLATE :: " + template.getTemplate());
			for(ElementState elem : template.getElements()){
				System.err.println("ELEMENT :: " + elem.getOuterHtml());
			}
			System.err.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
		}
		assertTrue(1 == template_elements.size());	}
}
