package database;

import org.testng.annotations.Test;

import com.qanairy.persistence.OrientConnectionFactory;

public class ConnectionFactoryTests {
	
	@Test
	public void assertConnectionNotNull(){
		OrientConnectionFactory connection = new OrientConnectionFactory();
		
		assert(connection!=null);
	}
}