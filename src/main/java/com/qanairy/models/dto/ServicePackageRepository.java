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
	 * 
	 * @PRE service_package.getKey() != null
	 */
	@Override
	public IServicePackage convertToRecord(OrientConnectionFactory connection, ServicePackage service_package) {
		service_package.setKey(generateKey(service_package));

		@SuppressWarnings("unchecked")
		Iterable<IServicePackage> svc_pkgs = (Iterable<IServicePackage>) DataAccessObject.findByKey(service_package.getKey(), connection, IServicePackage.class);
		Iterator<IServicePackage> iter = svc_pkgs.iterator();
		
		IServicePackage svc_pkg_record = null;
		if(!iter.hasNext()){		
			svc_pkg_record = connection.getTransaction().addVertex("class:"+IServicePackage.class.getCanonicalName()+","+UUID.randomUUID(), IServicePackage.class);
			svc_pkg_record.setKey(service_package.getKey());
			svc_pkg_record.setName(service_package.getName());
			svc_pkg_record.setPrice(service_package.getPrice());
			svc_pkg_record.setMaxUsers(service_package.getMaxUsers());
		}
		else {
			svc_pkg_record = iter.next();
		}
		

		return svc_pkg_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServicePackage create(OrientConnectionFactory connection, ServicePackage service_package) {
		service_package.setKey(generateKey(service_package));

		ServicePackage svc_pkg = find(connection, generateKey(service_package));
		
		if(svc_pkg == null){
			IServicePackage pkg_record = convertToRecord(connection, service_package);
			svc_pkg = convertFromRecord(pkg_record);
			connection.save();
		}

		return svc_pkg;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServicePackage update(OrientConnectionFactory connection, ServicePackage service_package) {
		@SuppressWarnings("unchecked")
		Iterable<IServicePackage> svc_pkgs = (Iterable<IServicePackage>) DataAccessObject.findByKey(service_package.getKey(), connection, IServicePackage.class);
		Iterator<IServicePackage> iter = svc_pkgs.iterator();
		
		if(iter.hasNext()){
			IServicePackage svc_pkg = iter.next();
			svc_pkg.setName(service_package.getName());
			svc_pkg.setMaxUsers(service_package.getMaxUsers());
			svc_pkg.setPrice(service_package.getPrice());
			connection.save();
		}
		
		return service_package;
	}
	
	/**
	 * Looks up the current object by key
	 * @param orient_connection
	 * @return
	 */
	@Override
	public ServicePackage find(OrientConnectionFactory orient_connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IServicePackage> svc_pkgs = (Iterable<IServicePackage>) DataAccessObject.findByKey(key, orient_connection, IServicePackage.class);
		Iterator<IServicePackage> iter = svc_pkgs.iterator();
		
		if(iter.hasNext()){
			return convertFromRecord(iter.next());
		}
		
		return null;
	}

	@Override
	public ServicePackage convertFromRecord(IServicePackage obj) {		
		ServicePackage pkg = new ServicePackage(obj.getKey(), obj.getName(), obj.getPrice(), obj.getMaxUsers());

		return pkg;
	}
}