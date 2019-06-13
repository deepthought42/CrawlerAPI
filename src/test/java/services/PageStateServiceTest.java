package services;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.qanairy.services.PageStateService;

public class PageStateServiceTest {
	PageStateService page_state_service = new PageStateService();
	
	@Test
	public void listTest(){
		List<String> list = new ArrayList<String>();
		list.add("hello");
		list.add("world");
		list.add("what");
		list.add("up");
		
		for(String item : list){
			item = "woah";
		}
		
		for(String item : list){
			System.err.println(item);
		}
	}
}
