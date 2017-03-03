package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.ServicePackage;
import com.qanairy.persistence.IServicePackage;

/**
 * 
 */
public class ServicePackageTests {
	
	/**
	 * 
	 */
	@Test
	public void servicePackageCreateRecord(){
		ServicePackage svc_pkg = new ServicePackage("Test Package", 100, 5);

		IServicePackage svc_pkg_record = svc_pkg.create();
		
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

		IServicePackage svc_pkg_record = svc_pkg.update();
		
		Assert.assertTrue(svc_pkg_record.getKey().equals(svc_pkg.getKey()));
		Assert.assertTrue(svc_pkg_record.getName().equals(svc_pkg.getName()));
		Assert.assertTrue(svc_pkg_record.getPrice() == svc_pkg.getPrice());
		Assert.assertTrue(svc_pkg_record.getMaxUsers() == svc_pkg.getMaxUsers());
	}
}
