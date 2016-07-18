package com.minion.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.UUID;

import com.minion.memory.ObjectDefinition;
import com.minion.memory.OrientDbPersistor;
import com.minion.memory.SequenceVertex;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.minion.browsing.ActionFactory;
import com.minion.browsing.Page;
import com.minion.browsing.PageElement;

public class SequenceVertexTests {

	@BeforeTest
	public void setUp(){
		
	}
	
	@Test
	public void SequenceVertexPageSaveTest() throws MalformedURLException, IOException {
		WebDriver driver = new FirefoxDriver();
		driver.get("localhost:3000");
		Page page = new Page(driver, DateFormat.getDateTimeInstance());
		SequenceVertex sequenceVertex = new SequenceVertex(page.hashCode(), page.toString(), page.getClass().getCanonicalName().toString(), "");
		OrientDbPersistor<SequenceVertex> persistor = new OrientDbPersistor<SequenceVertex>();
		persistor.findAndUpdateOrCreate(sequenceVertex, ActionFactory.getActions());
		
		driver.close();
	}
	
	@AfterTest
	public void tearDown(){
		
	}

}
