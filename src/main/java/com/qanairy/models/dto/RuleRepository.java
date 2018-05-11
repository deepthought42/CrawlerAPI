package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.PageElement;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IRule;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.rules.Rule;
import com.qanairy.rules.RuleFactory;

/**
 *
 */
public class RuleRepository implements IPersistable<Rule, IRule> {
	private static Logger log = LoggerFactory.getLogger(PageElement.class);

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(Rule rule) {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(rule.getType().toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IRule save(OrientConnectionFactory connection, Rule rule) {
		@SuppressWarnings("unchecked")
		Iterable<IRule> rule_records = (Iterable<IRule>) DataAccessObject.findByKey(rule.getType().toString(), connection, IRule.class);
		Iterator<IRule> iter = rule_records.iterator();
		
		if( !iter.hasNext()){
			IRule rule_record = connection.getTransaction().addVertex("class:"+IRule.class.getSimpleName()+","+UUID.randomUUID(), IRule.class);
			rule_record.setKey(rule.getType().toString());
			rule_record.setType(rule.getType().toString());
			rule_record.setValue(rule.getValue());
			return rule_record;
		}
		
		return iter.next();
	}

	@Override
	public Rule load(IRule rule_record) {
		/**  call rule factory to get rule built */
		return RuleFactory.build(rule_record.getType(), rule_record.getValue());
	}

	@Override
	public Rule find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IRule> svc_pkgs = (Iterable<IRule>) DataAccessObject.findByKey(key, connection, IRule.class);
		Iterator<IRule> iter = svc_pkgs.iterator();

		if(iter.hasNext()){
			return load(iter.next());
		}

		return null;
	}

	@Override
	public List<Rule> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}
