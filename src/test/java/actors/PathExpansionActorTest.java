package actors;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.qanairy.models.PageElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;

public class PathExpansionActorTest {

	@Mock
	private Test test;
	
	@Mock 
	private PageState page_state;
	
	@Mock 
	private PageState page_state_1;
	
	@Mock
	private PageElementState page_element;
	
	@Mock
	private PageElementState page_element_1;
	
	@Before
	public void setup(){
        MockitoAnnotations.initMocks(this);
	}
	/*
	@org.junit.Test
	public void doesElementExistInMultiplePageStatesWithinTestIsFalseWhenOnlyOnePage(){
		List<PathObject> path = new ArrayList<PathObject>();
		path.add(page_state);
		path.add(page_element);
		
		Set<PageElementState> elements = new HashSet<PageElementState>();
		elements.add(page_element);
		
		when(page_state.getElements()).thenReturn(elements);
		when(page_element.getKey()).thenReturn("temp_key");
		when(test.getPathObjects()).thenReturn(path);
		PathExpansionActor actor = new PathExpansionActor();
		boolean exists = actor.doesElementExistInMultiplePageStatesWithinTest(test, page_element);
		assertFalse(exists);
	}
	
	@org.junit.Test
	public void doesElementExistInMultiplePageStatesWithinTestIsFalseWhenTwoDifferentPages(){
		List<PathObject> path = new ArrayList<PathObject>();
		path.add(page_state);
		path.add(page_state_1);
		path.add(page_element);
		
		Set<PageElementState> elements = new HashSet<PageElementState>();
		elements.add(page_element);
		
		Set<PageElementState> elements_1 = new HashSet<PageElementState>();
		elements_1.add(page_element_1);
		
		when(test.getPathObjects()).thenReturn(path);

		when(page_state.getElements()).thenReturn(elements);
		when(page_state_1.getElements()).thenReturn(elements_1);
		
		when(page_element.getKey()).thenReturn("temp_key");
		when(page_element_1.getKey()).thenReturn("page state");
		
		
		PathExpansionActor actor = new PathExpansionActor();
		boolean exists = actor.doesElementExistInMultiplePageStatesWithinTest(test, page_element);
		assertFalse(exists);
	}
	
	@org.junit.Test
	public void doesElementExistInMultiplePageStatesWithinTestIsTrue(){
		List<PathObject> path = new ArrayList<PathObject>();
		path.add(page_state);
		path.add(page_state_1);
		path.add(page_element);
		
		Set<PageElementState> elements = new HashSet<PageElementState>();
		elements.add(page_element);
		
		Set<PageElementState> elements_1 = new HashSet<PageElementState>();
		elements_1.add(page_element);
		
		when(page_state.getElements()).thenReturn(elements);
		when(page_state_1.getElements()).thenReturn(elements_1);
		
		when(page_element.getKey()).thenReturn("temp_key");
		when(test.getPathObjects()).thenReturn(path);
		
		PathExpansionActor actor = new PathExpansionActor();
		boolean exists = actor.doesElementExistInMultiplePageStatesWithinTest(test, page_element);
		assertTrue(exists);
	}
	*/
}
