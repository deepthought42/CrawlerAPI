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
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestRecord;

public class TestDaoImpl implements TestDao {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestDao.class);

	@Override
	public Test save(Test test) {
		assert test != null;
		
		test.setKey(generateKey(test));
		Test test_record = find(test.getKey());
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(test_record == null){
			test_record = connection.getTransaction().addFramedVertex(Test.class);
			test_record.setKey(generateKey(test));
			
			test_record.setPathKeys(test.getPathKeys());
			
			PageStateDao page_record = new PageStateDaoImpl();
			test_record.setResult(page_record.save(test.getResult()));
			
			PathObjectDao path_obj_dao = new PathObjectDaoImpl();
			for(PathObject obj: test.getPathObjects()){
				PathObject path_obj = path_obj_dao.save(obj);
				test_record.addPathObject(path_obj);
			}
		
			/*
			DomainRepository domain_record = new DomainRepository();
			IDomain idomain = domain_record.save(test.getDomain());
			test_record.addDomain(idomain);
			 */
		}
		TestRecordDao test_record_record = new TestRecordDaoImpl();

		for(TestRecord record : test.getRecords()){
			test_record.addRecord(test_record_record.save(record));
		}
		
		List<Group> groups = new ArrayList<Group>();
		GroupDao group_repo = new GroupDaoImpl();
		for(Group group : test.getGroups()){
			groups.add(group_repo.save(group));
		}
		test_record.setGroups(groups);
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
	
	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	public String generateKey(Test test) {
		String path_key =  String.join("::", test.getPathKeys());
		
		PageStateDaoImpl page_dao = new PageStateDaoImpl();
		path_key += page_dao.generateKey(test.getResult());
		
		return path_key;
	}
}
