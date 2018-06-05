package com.qanairy.models.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.commons.collections.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.dao.GroupDao;
import com.qanairy.models.dao.PageStateDao;
import com.qanairy.models.dao.PathObjectDao;
import com.qanairy.models.dao.TestDao;
import com.qanairy.models.dao.TestRecordDao;
import com.qanairy.persistence.Group;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestRecord;

public class TestDaoImpl implements TestDao {
	private static Logger log = LoggerFactory.getLogger(TestDao.class);

	@Override
	public Test save(Test test) {
		assert test != null;
		
		Test test_record = find(test.getKey());
		System.err.println("SAVING TEST NOW      !!!!!!!!!!!!    ##############     !!!!!!!!!!!!!!!  ");
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(test_record == null){
			test_record = connection.getTransaction().addFramedVertex(Test.class);
			test_record.setKey(test.getKey());
			test_record.setPathKeys(test.getPathKeys());
			test_record.setSpansMultipleDomains(test.getSpansMultipleDomains());
			PageStateDao page_record = new PageStateDaoImpl();
			test_record.setResult(page_record.save(test.getResult()));
			
			PathObjectDao path_obj_dao = new PathObjectDaoImpl();
			System.err.println("Total path objects about to be saved :: "+test.getPathObjects().size());
			for(PathObject obj: test.getPathObjects()){
				System.err.println("Path object type :: "+obj.getType());
				System.err.println("Path Object about to be saved :: "+obj);
				PathObject path_obj = path_obj_dao.save(obj);
				test_record.addPathObject(path_obj);
			}
		
			/*
			DomainRepository domain_record = new DomainRepository();
			IDomain idomain = domain_record.save(test.getDomain());
			test_record.addDomain(idomain);
			 */
		}
		
		TestRecordDao test_record_dao = new TestRecordDaoImpl();
		for(TestRecord record : test.getRecords()){

			boolean exists = false;
			for(TestRecord record2 : test_record.getRecords()){
				if(record2.getKey().equals(record.getKey())){
					exists = true;
				}
			}
			
			if(!exists){
				test_record.addRecord(test_record_dao.save(record));
			}
		}
		
		GroupDao group_repo = new GroupDaoImpl();
		for(Group group : test.getGroups()){
			boolean exists = false;
			for(Group group2 : test_record.getGroups()){
				if(group2.getKey().equals(group.getKey())){
					exists = true;
				}
			}
			
			if(!exists){
				test_record.addGroup(group_repo.save(group));
			}
		}

		test_record.setLastRunTimestamp(test.getLastRunTimestamp());
		test_record.setRunTime(test.getRunTime());
		test_record.setName(test.getName());
		test_record.setCorrect(test.getCorrect());
		test_record.setBrowserStatuses(test.getBrowserStatuses());
		
		return test_record;
	}

	@Override
	public Test find(String key) {
		Test test = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			test = connection.getTransaction().getFramedVertices("key", key, Test.class).next();
		}catch(NoSuchElementException e){
			log.error("Error requesting test record from database");
		}
		connection.close();
		return test;
	}
	
	public List<Test> findByUrl(String url){
		OrientConnectionFactory connection = new OrientConnectionFactory();
		List<Test> tests = IteratorUtils.toList(connection.getTransaction().getFramedVertices("url", url, Test.class));
		connection.close();
		return tests;
	}

	/**
	 * {@inheritDoc}
	 */
	public Test findByName(String name) {
		Test test = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			test = connection.getTransaction().getFramedVertices("name", name, Test.class).next();
		}catch(NoSuchElementException e){
			log.error("Error requesting test record from database");
		}
		connection.close();
		return test;
	}
}
