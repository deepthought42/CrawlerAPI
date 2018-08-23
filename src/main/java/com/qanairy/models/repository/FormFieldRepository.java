package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.minion.browsing.form.FormField;

public interface FormFieldRepository extends Neo4jRepository<FormField, Long> {

}
