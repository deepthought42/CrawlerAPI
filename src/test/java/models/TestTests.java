package models;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.minion.browsing.Crawler;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestService;

public class TestTests {
	@Mock
	private PageState page_state;
	
	@Mock
	private PageState page_state1;
	
	@Mock
	private Test test;
	
	@Mock
	private Crawler crawler;
	
	@Spy
	private TestService test_service;
	
	@Mock
	private BrowserService browser_service;
	
	@Before
	public void setUp(){
		MockitoAnnotations.initMocks(this);
		test_service.init(crawler);
	}
	
	@org.junit.Test
	public void generateTestNameTestWithLongPath() throws MalformedURLException{
		
		List<PathObject> objects = new ArrayList<>();
		PageState page = new PageState();
		page.setUrl("https://test.tester.com/services/test-service.html");
		objects.add(page);
		
		ElementState element = new ElementState();
		element.setName("a");
		
		Action action = new Action();
		action.setName("click");
		
		objects.add(element);
		objects.add(action);
		
		Test test = new Test();
		test.setPathObjects(objects);
		
		String name = test.generateTestName();
		System.err.println("TEST NAME :: "+name);
		assertEquals("services test-service.html page link click", name);
	}
	
	@org.junit.Test
	public void generateTestNameWithNoPath() throws MalformedURLException{
		
		List<PathObject> objects = new ArrayList<>();
		PageState page = new PageState();
		page.setUrl("https://test.tester.com/");
		objects.add(page);
		
		ElementState element = new ElementState();
		element.setName("a");
		
		Action action = new Action();
		action.setName("click");
		
		objects.add(element);
		objects.add(action);
		
		Test test = new Test();
		test.setPathObjects(objects);
		
		String name = test.generateTestName();
		System.err.println("TEST NAME :: "+name);
		assertEquals("home page link click", name);
	}
	
	@org.junit.Test
	public void generateTestNameWithElementThatHasIdAttribute() throws MalformedURLException{
		
		List<PathObject> objects = new ArrayList<>();
		PageState page = new PageState();
		page.setUrl("https://test.tester.com/");
		objects.add(page);
		
		ElementState element = new ElementState();
		List<String> attribute_vals = new ArrayList<>();
		attribute_vals.add("id-attr-1");
		element.addAttribute(new Attribute("id", attribute_vals));
		element.setName("a");
		
		Action action = new Action();
		action.setName("click");
		
		objects.add(element);
		objects.add(action);
		
		Test test = new Test();
		test.setPathObjects(objects);
		
		String name = test.generateTestName();
		System.err.println("TEST NAME :: "+name);
		assertEquals("home page id-attr-1 click", name);
	}
}
