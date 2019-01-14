package services;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.mockito.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.qanairy.api.exceptions.PagesAreNotMatchingException;
import com.qanairy.models.PageState;
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
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private BrowserService browser_service;
	
	@Before
	public void setup(){
		MockitoAnnotations.initMocks(this);
		test_service.init(crawler, browser_service);
	}
	
	@org.junit.Test
	public void runTestInFirefoxIsPassingWhenExpectedResult() throws GridException, WebDriverException, NoSuchAlgorithmException, PagesAreNotMatchingException, IOException {
		when(browser_service.getConnection(Matchers.anyString())).thenReturn(new Browser());
		when(page_state.getKey()).thenReturn("valid_key");
		when(crawler.crawlPath(Matchers.anyList(), Matchers.anyList(), Matchers.any(), Matchers.any())).thenReturn(page_state);
		
		when(test.getResult()).thenReturn(page_state);
		TestRecord record = test_service.runTest(test, "firefox", TestStatus.PASSING);
		
		assertNotNull(record);
	}
}
