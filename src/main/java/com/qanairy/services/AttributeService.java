package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Attribute;
import com.qanairy.models.repository.AttributeRepository;

@Service
public class AttributeService {

	@Autowired
	private AttributeRepository attribute_repo;
	
	/**
	 * @param attribute {@link Attribute} to be saved
	 * 
	 * @return
	 * 
	 * @pre attribute != null
	 */
	public Attribute save(Attribute attribute){
		assert attribute != null;
		
		Attribute attribute_record = findByKey(attribute.getKey());
		if(attribute_record == null){
			attribute_record = attribute_repo.save(attribute);
		}
		
		return attribute_record;
	}
	
	public Attribute findByKey(String key){
		assert key != null;
		assert !key.isEmpty();
		
		return attribute_repo.findByKey(key);
	}
}
