package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.ServicePackage;
import com.qanairy.persistence.IServicePackage;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Verifies the {@link ServicePackage} class
 */
public class ServicePackageTests {
	
	/**
	 * 
	 */
	@Test
	public void servicePackageCreateRecord(){
		ServicePackage svc_pkg = new ServicePackage("Test Package", 100, 5);

		IServicePackage svc_pkg_record = svc_pkg.create(new OrientConnectionFactory());
		
		Assert.assertTrue(svc_pkg_record.getKey().equals(svc_pkg.getKey()));
		Assert.assertTrue(svc_pkg_record.getName().equals(svc_pkg.getName()));
		Assert.assertTrue(svc_pkg_record.getPrice() == svc_pkg.getPrice());
		Assert.assertTrue(svc_pkg_record.getMaxUsers() == svc_pkg.getMaxUsers());
	}
	
	/**
	 * 
	 */
	@Test
	public void servicePackageUpdateRecord(){
		ServicePackage svc_pkg = new ServicePackage("Test Package", 80, 5);

		IServicePackage svc_pkg_record = svc_pkg.update(new OrientConnectionFactory());
		
		Assert.assertTrue(svc_pkg_record.getKey().equals(svc_pkg.getKey()));
		Assert.assertTrue(svc_pkg_record.getName().equals(svc_pkg.getName()));
		Assert.assertTrue(svc_pkg_record.getPrice() == svc_pkg.getPrice());
		Assert.assertTrue(svc_pkg_record.getMaxUsers() == svc_pkg.getMaxUsers());
	}
	
	/**
	 * 
	 */
	@Test
	public void servicePackageFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		ServicePackage svc_pkg = new ServicePackage("Find Test Package", 80, 5);
		svc_pkg.create(orient_connection);
		IServicePackage svc_pkg_record = svc_pkg.find(orient_connection);
		
		Assert.assertTrue(svc_pkg_record.getKey().equals(svc_pkg.getKey()));
		Assert.assertTrue(svc_pkg_record.getName().equals(svc_pkg.getName()));
		Assert.assertTrue(svc_pkg_record.getMaxUsers() == svc_pkg.getMaxUsers());
		Assert.assertTrue(svc_pkg_record.getPrice() == svc_pkg.getPrice());
	}
}
