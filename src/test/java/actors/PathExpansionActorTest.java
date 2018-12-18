package actors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.minion.actors.PathExpansionActor;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;

public class PathExpansionActorTest {

	@Mock
	private Test test;
	
	@Mock 
	private PageState page_state;
	
	@Mock 
	private PageState page_state_1;
	
	@Mock
	private PageElement page_element;
	
	@Mock
	private PageElement page_element_1;
	
	@Before
	public void setup(){
        MockitoAnnotations.initMocks(this);
	}
	
	@org.junit.Test
	public void doesElementExistInMultiplePageStatesWithinTestIsFalseWhenOnlyOnePage(){
		List<PathObject> path = new ArrayList<PathObject>();
		path.add(page_state);
		path.add(page_element);
		
		Set<PageElement> elements = new HashSet<PageElement>();
		elements.add(page_element);
		
		when(page_state.getElements()).thenReturn(elements);
		when(page_element.getKey()).thenReturn("temp_key");
		when(test.getPathObjects()).thenReturn(path);
		boolean exists = PathExpansionActor.doesElementExistInMultiplePageStatesWithinTest(test, page_element);
		assertFalse(exists);
	}
	
	@org.junit.Test
	public void doesElementExistInMultiplePageStatesWithinTestIsFalseWhenTwoDifferentPages(){
		List<PathObject> path = new ArrayList<PathObject>();
		path.add(page_state);
		path.add(page_state_1);
		path.add(page_element);
		
		Set<PageElement> elements = new HashSet<PageElement>();
		elements.add(page_element);
		
		Set<PageElement> elements_1 = new HashSet<PageElement>();
		elements_1.add(page_element_1);
		
		when(test.getPathObjects()).thenReturn(path);

		when(page_state.getElements()).thenReturn(elements);
		when(page_state_1.getElements()).thenReturn(elements_1);
		
		when(page_element.getKey()).thenReturn("temp_key");
		when(page_element_1.getKey()).thenReturn("page state");
		
		
		boolean exists = PathExpansionActor.doesElementExistInMultiplePageStatesWithinTest(test, page_element);
		assertFalse(exists);
	}
	
	@org.junit.Test
	public void doesElementExistInMultiplePageStatesWithinTestIsTrue(){
		List<PathObject> path = new ArrayList<PathObject>();
		path.add(page_state);
		path.add(page_state_1);
		path.add(page_element);
		
		Set<PageElement> elements = new HashSet<PageElement>();
		elements.add(page_element);
		
		Set<PageElement> elements_1 = new HashSet<PageElement>();
		elements_1.add(page_element);
		
		when(page_state.getElements()).thenReturn(elements);
		when(page_state_1.getElements()).thenReturn(elements_1);
		
		when(page_element.getKey()).thenReturn("temp_key");
		when(test.getPathObjects()).thenReturn(path);
		
		boolean exists = PathExpansionActor.doesElementExistInMultiplePageStatesWithinTest(test, page_element);
		assertTrue(exists);
	}
}
