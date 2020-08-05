package models;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import com.qanairy.models.Action;
import com.qanairy.models.ElementState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.PageState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.Test;

public class TestTests {
	
	@org.junit.Test
	public void generateTestNameTestWithLongPath() throws MalformedURLException{
		
		List<LookseeObject> objects = new ArrayList<>();
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
		
		List<LookseeObject> objects = new ArrayList<>();
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
		
		List<LookseeObject> objects = new ArrayList<>();
		PageState page = new PageState();
		page.setUrl("https://test.tester.com/");
		objects.add(page);
		
		ElementState element = new ElementState();
		element.addAttribute("id", "id-attr-1");
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
