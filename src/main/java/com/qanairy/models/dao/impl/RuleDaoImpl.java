package com.qanairy.models.dao.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.qanairy.models.dao.RuleDao;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.Rule;

/**
 * 
 */
public class RuleDaoImpl implements RuleDao {

	@Override
	public void save(Rule rule) {
		rule.setKey(generateKey(rule));
		Rule record = find(rule.getKey());
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(record == null){
			record = connection.getTransaction().addFramedVertex(Rule.class);
			record.setKey(rule.getKey());
			record.setType(rule.getType());
			record.setValue(rule.getValue());
		}
		connection.close();
	}

	@Override
	public Rule find(String key) {
		Rule record = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			record = connection.getTransaction().getFramedVertices("key", key, Rule.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find record");
		}
		connection.close();
		return record;
	}

	@Override
	public Iterator<? extends Rule> findAll() {
		Iterator<? extends Rule> records = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		records = connection.getTransaction().getFramedVertices(Rule.class);

		connection.close();
		return records;
	}


	private String generateKey(Rule rule) {
		return rule.getType().toString();
	}
}
