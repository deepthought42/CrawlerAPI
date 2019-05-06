package models;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;

@SpringBootTest
public class ExploratoryPathTests {
	
	@Mock
	private PageState page;

	@Before
	public void start(){
        MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void verifyEmptyPathReturnsFalse(){
		List<PathObject> path_objects = new ArrayList<>();
		
		when(page.getKey()).thenReturn("this is a test key");
		
		boolean isCycle = ExploratoryPath.hasCycle(path_objects, page);
		assertFalse(isCycle);

	}
	
	@Test
	public void verifySingleNodePathReturnsFalse(){
		when(page.getKey()).thenReturn("this is a test key");
		List<PathObject> path_objs= new ArrayList<>();
		path_objs.add(page);
		System.err.println("page  : " + page);
		boolean isCycle = ExploratoryPath.hasCycle(path_objs, page);
		assertFalse(isCycle);
	}
	
	@Test
	public void verifyTwoConsecutiveEqualsNodeInPathReturnsTrue(){
		when(page.getKey()).thenReturn("this is a test key");
		page.setKey("this is a test key");
		List<PathObject> path_objs = new ArrayList<>();
		path_objs.add(page);
		path_objs.add(page);

		System.err.println("page  : " + page);
		boolean isCycle = ExploratoryPath.hasCycle(path_objs, page);
		assertTrue(isCycle);
	}
	
	@Test
	public void verifyTwoEqualsNodeSeparatedByNullInPathReturnsTrue(){
		when(page.getKey()).thenReturn("this is a test key");
		List<PathObject> path_objs = new ArrayList<>();
		path_objs.add(page);
		path_objs.add(null);
		path_objs.add(page);

		System.err.println("page  : " + page);
		boolean isCycle = ExploratoryPath.hasCycle(path_objs, page);
		assertTrue(isCycle);
	}
}
