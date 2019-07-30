package services;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;

import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.qanairy.api.exceptions.PagesAreNotMatchingException;
import com.qanairy.models.Action;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestService;

@SpringBootTest
public class TestServiceTest {

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
		test_service.init(crawler, browser_service);
	}
	
	public void runTestIsPassingWhenExpectedResult() throws GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, IOException, InterruptedException, ExecutionException {
		when(browser_service.getConnection(Matchers.anyString(), Matchers.any())).thenReturn(new Browser());
		when(page_state.getKey()).thenReturn("valid_key");		 
		//when(crawler.crawlPath(Matchers.anyList(), Matchers.anyList(), Matchers.any(), Matchers.any()), Matchers.int).thenReturn(page_state);

		when(test.getResult()).thenReturn(page_state);
		TestRecord record = test_service.runTest(test, "firefox", TestStatus.PASSING);
		
		assertNotNull(record);
		assertTrue(record.getStatus().equals(TestStatus.PASSING));
	}
	
	public void runTestIsPassingWhenNotExpectedResult() throws GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, IOException, InterruptedException, ExecutionException {
		when(browser_service.getConnection(Matchers.anyString(), Matchers.any())).thenReturn(new Browser());
		when(page_state.getKey()).thenReturn("valid_key");
		when(page_state1.getKey()).thenReturn("invalid_key");
		when(crawler.crawlPath(Matchers.anyList(), Matchers.anyList(), Matchers.any(), Matchers.any(), Matchers.anyMap(), Matchers.anyList())).thenReturn(page_state1);
		
		when(test.getResult()).thenReturn(page_state);
		TestRecord record = test_service.runTest(test, "firefox", TestStatus.PASSING);
		
		assertNotNull(record);
		assertTrue(record.getStatus().equals(TestStatus.FAILING));
	}
	
	@org.junit.Test
	public void generateTestNameTest() throws MalformedURLException{
		
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
		
		String name = TestService.generateTestName(test);
		System.err.println("TEST NAME :: "+name);
		assertEquals("services test-service.html page link click", name);
	}
}
