package models;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import com.looksee.models.ActionOLD;
import com.looksee.models.Element;
import com.looksee.models.LookseeObject;
import com.looksee.models.PageState;
import com.looksee.models.Test;

public class TestTests {
	
	@org.junit.Test
	public void generateTestNameTestWithLongPath() throws MalformedURLException{
		
		List<LookseeObject> objects = new ArrayList<>();
		PageState page = new PageState();
		page.setUrl("https://test.tester.com/services/test-service.html");
		objects.add(page);
		
		Element element = new Element();
		element.setName("a");
		
		ActionOLD action = new ActionOLD();
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
		
		List<LookseeObject> objects = new ArrayList<>();
		PageState page = new PageState();
		page.setUrl("https://test.tester.com/");
		objects.add(page);
		
		Element element = new Element();
		element.setName("a");
		
		ActionOLD action = new ActionOLD();
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
		
		List<LookseeObject> objects = new ArrayList<>();
		PageState page = new PageState();
		page.setUrl("https://test.tester.com/");
		objects.add(page);
		
		Element element = new Element();
		element.addAttribute("id", "id-attr-1");
		element.setName("a");
		
		ActionOLD action = new ActionOLD();
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
