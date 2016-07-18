package com.minion.test;

import java.io.IOException;
import java.net.MalformedURLException;

import com.minion.memory.ObjectDefinition;
import com.minion.memory.OrientDbPersistor;

import org.testng.annotations.Test;

import com.minion.browsing.ActionFactory;
import com.minion.browsing.PageElement;

public class OrientDbPersistorTests {
  @Test
  public void confirmFindWorksForObjectDefinition() {
	  
  }

  /**
   * Tests that object definition is able to be found and updated or saved without exception.
   * 
   * @throws MalformedURLException
   * @throws IOException
   */
	@Test
	public void ObjectDefinitionSaveTest() throws MalformedURLException, IOException {
		ObjectDefinition obj = new ObjectDefinition("div", PageElement.class.getSimpleName() );
		OrientDbPersistor persistor = new OrientDbPersistor();
		persistor.findAndUpdateOrCreate(obj, ActionFactory.getActions());
	}
}
