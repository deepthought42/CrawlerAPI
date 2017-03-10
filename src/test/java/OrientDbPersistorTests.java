
import java.io.IOException;
import java.net.MalformedURLException;

import com.qanairy.models.PageElement;

import com.qanairy.persistence.OrientDbPersistor;
import com.qanairy.rl.memory.ObjectDefinition;

import org.testng.annotations.Test;

import com.minion.browsing.ActionFactory;

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
		try {
			persistor.findAndUpdateOrCreate(obj, ActionFactory.getActions());
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
