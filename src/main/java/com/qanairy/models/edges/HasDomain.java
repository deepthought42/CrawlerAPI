package com.qanairy.models.edges;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import com.qanairy.models.Account;
import com.qanairy.models.Domain;

@RelationshipEntity
public class HasDomain {
 
    @Id
    Long id;
 
    //private Collection<String> roles;
    @StartNode
    private Account account;
    
    @EndNode
    private Domain domain;
    
    public Domain getDomain(){
    	return this.domain;
    }
    
    public void setDomain(Domain domain){
    	this.domain = domain;
    }
    
    public Account getAccount(){
    	return this.account;
    }
    
    public void setAccount(Account account){
    	this.account = account;
    }
}