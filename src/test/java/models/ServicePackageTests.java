package models;

import org.testng.Assert;
import org.testng.annotations.Test;
import com.qanairy.models.ServicePackage;
import com.qanairy.models.dto.ServicePackageRepository;
import com.qanairy.persistence.IServicePackage;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Verifies the {@link ServicePackage} class
 */
public class ServicePackageTests {
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void servicePackageCreateRecord(){
		ServicePackage svc_pkg = new ServicePackage("Test Package", 100, 5);

		ServicePackageRepository sp_repo = new ServicePackageRepository();
		IServicePackage svc_pkg_record = sp_repo.save(new OrientConnectionFactory(), svc_pkg);
		
		Assert.assertTrue(svc_pkg_record.getKey().equals(svc_pkg.getKey()));
		Assert.assertTrue(svc_pkg_record.getName().equals(svc_pkg.getName()));
		Assert.assertTrue(svc_pkg_record.getPrice() == svc_pkg.getPrice());
		Assert.assertTrue(svc_pkg_record.getMaxUsers() == svc_pkg.getMaxUsers());
	}
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void servicePackageUpdateRecord(){
		ServicePackage svc_pkg = new ServicePackage("Update Test Package", 100, 10);
		ServicePackageRepository sp_repo = new ServicePackageRepository();
		IServicePackage svc_pkg_record_create = sp_repo.save(new OrientConnectionFactory(), svc_pkg);
		svc_pkg_record_create.setMaxUsers(5);
		svc_pkg_record_create.setPrice(75);
		IServicePackage svc_pkg_record_update = sp_repo.save(new OrientConnectionFactory(), sp_repo.load(svc_pkg_record_create));
		
		Assert.assertTrue(svc_pkg_record_update.getKey().equals(svc_pkg_record_create.getKey()));
		Assert.assertTrue(svc_pkg_record_update.getName().equals(svc_pkg_record_create.getName()));
		Assert.assertTrue(svc_pkg_record_update.getPrice() == svc_pkg_record_create.getPrice());
		Assert.assertTrue(svc_pkg_record_update.getMaxUsers() == svc_pkg_record_create.getMaxUsers());
	}
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void servicePackageFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		ServicePackageRepository sp_repo = new ServicePackageRepository();

		ServicePackage svc_pkg = new ServicePackage("Find Test Package", 80, 5);
		sp_repo.save(orient_connection, svc_pkg);
		
		ServicePackage svc_pkg_record = sp_repo.find(orient_connection, svc_pkg.getKey());
		
		Assert.assertTrue(svc_pkg_record.getKey().equals(svc_pkg.getKey()));
		Assert.assertTrue(svc_pkg_record.getName().equals(svc_pkg.getName()));
		Assert.assertTrue(svc_pkg_record.getMaxUsers() == svc_pkg.getMaxUsers());
		Assert.assertTrue(svc_pkg_record.getPrice() == svc_pkg.getPrice());
	}
}
