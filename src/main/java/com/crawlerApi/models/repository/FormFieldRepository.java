package com.crawlerApi.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.crawlerApi.browsing.form.FormField;

public interface FormFieldRepository extends Neo4jRepository<FormField, Long> {

}
