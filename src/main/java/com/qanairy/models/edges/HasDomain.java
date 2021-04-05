package com.qanairy.models.edges;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;

import com.qanairy.models.Domain;

@RelationshipEntity
public class HasDomain {
 
    @Id
    Long id;
 
    //private Collection<String> roles;

    @EndNode
    private Domain domain;
    
    public Domain getDomain(){
    	return this.domain;
    }
    
    public void setDomain(Domain domain){
    	this.domain = domain;
    }
}