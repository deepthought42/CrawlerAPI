package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.qanairy.models.Form;
import org.springframework.data.repository.query.Param;

public interface FormRepository extends Neo4jRepository<Form, Long> {

	public Form findByKey(@Param("key") String key);
}
