package models;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.PageState;

@SpringBootTest
public class ExploratoryPathTests {
	
	@Mock
	private PageState page;
	
    @Mock 
    private MessageDigest messageDigestMock;

	@Before
	public void start(){
        MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void verifyEmptyPathReturnsFalse(){
		List<String> path_key_list = new ArrayList<>();
		when(page.getKey()).thenReturn("this is a test key");

		boolean isCycle = ExploratoryPath.hasCycle(path_key_list, page);
		assertFalse(isCycle);

	}
	
	@Test
	public void verifySingleNodePathReturnsFalse(){
		when(page.getKey()).thenReturn("this is a test key");
		List<String> path_key_list = new ArrayList<>();
		path_key_list.add(page.getKey());
		System.err.println("page  : " + page);
		boolean isCycle = ExploratoryPath.hasCycle(path_key_list, page);
		assertFalse(isCycle);
	}
	
	@Test
	public void verifyTwoConsecutiveEqualsNodeInPathReturnsTrue(){
		when(page.getKey()).thenReturn("this is a test key");
		page.setKey("this is a test key");
		List<String> path_key_list = new ArrayList<>();
		path_key_list.add(page.getKey());
		path_key_list.add(page.getKey());

		System.err.println("page  : " + page);
		boolean isCycle = ExploratoryPath.hasCycle(path_key_list, page);
		assertTrue(isCycle);
	}
	
	@Test
	public void verifyTwoEqualsNodeSeparatedByNullInPathReturnsTrue(){
		when(page.getKey()).thenReturn("this is a test key");
		List<String> path_key_list = new ArrayList<>();
		path_key_list.add(page.getKey());
		path_key_list.add(null);
		path_key_list.add(page.getKey());

		System.err.println("page  : " + page);
		boolean isCycle = ExploratoryPath.hasCycle(path_key_list, page);
		assertTrue(isCycle);
	}
}
