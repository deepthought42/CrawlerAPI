package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import com.qanairy.models.ServicePackage;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IServicePackage;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class ServicePackageRepository implements IPersistable<ServicePackage, IServicePackage> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(ServicePackage service_package) {
		return service_package.getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IServicePackage convertToRecord(OrientConnectionFactory connection, ServicePackage service_package) {
		service_package.setKey(generateKey(service_package));
		
		IServicePackage svc_pkg = connection.getTransaction().addVertex("class:"+IServicePackage.class.getCanonicalName()+","+UUID.randomUUID(), IServicePackage.class);
		svc_pkg.setKey(service_package.getKey());
		svc_pkg.setName(service_package.getName());
		svc_pkg.setPrice(service_package.getPrice());
		svc_pkg.setMaxUsers(service_package.getMaxUsers());

		return svc_pkg;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServicePackage create(OrientConnectionFactory connection, ServicePackage service_package) {
		IServicePackage svc_pkg = find(connection, generateKey(service_package));

		if(svc_pkg != null){
			svc_pkg = convertToRecord(connection, service_package);
			connection.save();
		}
		return service_package;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServicePackage update(OrientConnectionFactory connection, ServicePackage service_package) {
		IServicePackage svc_pkg = find(connection, service_package.getKey());
		  
		if(!svc_pkg.equals(null)){
			svc_pkg.setName(service_package.getName());
			svc_pkg.setMaxUsers(service_package.getMaxUsers());
			svc_pkg.setPrice(service_package.getPrice());
		}
		
		connection.save();
		
		return service_package;
	}
	
	/**
	 * Looks up the current object by key
	 * @param orient_connection
	 * @return
	 */
	@Override
	public IServicePackage find(OrientConnectionFactory orient_connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IServicePackage> svc_pkgs = (Iterable<IServicePackage>) DataAccessObject.findByKey(key, orient_connection, IServicePackage.class);
		Iterator<IServicePackage> iter = svc_pkgs.iterator();
		
		IServicePackage service_package_record = null; 
		if(iter.hasNext()){
			service_package_record = iter.next();
		}
		
		return service_package_record;
	}

	@Override
	public ServicePackage convertFromRecord(IServicePackage obj) {		
		ServicePackage pkg = new ServicePackage(obj.getName(), obj.getPrice(), obj.getMaxUsers());

		return pkg;
	}
}