package com.qanairy.models.edges;

import java.util.Collection;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import com.qanairy.models.Account;
import com.qanairy.models.Domain;

@RelationshipEntity(type = "HAS_DOMAIN")
public class HasDomain {
 
    @GraphId
    Long id;
 
    //private Collection<String> roles;
 
    @StartNode
    private Account account;
 
    @EndNode
    private Domain domain;
 
    // standard constructor, getters and setters 
    public Account getAccount(){
    	return this.account;
    }
    
    public void setAccount(Account account){
    	this.account = account;
    }
    
    public Domain getDomain(){
    	return this.domain;
    }
    
    public void setDomain(Domain domain){
    	this.domain = domain;
    }
}