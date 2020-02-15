package com.qanairy.models;

import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Path implements Persistable {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Path.class);
	
    @GeneratedValue
    @Id
	private Long id;
    
    @Relationship(type = "HAS")
    private List<PathSnippet> snippets;

	public List<PathSnippet> getSnippets() {
		return snippets;
	}

	public void setSnippets(List<PathSnippet> snippets) {
		this.snippets = snippets;
	}
	
	/**
	 * Iterates over list of snippets and places them in order by matching starting page states with ending page states
	 */
	public void orderSnippets() {
		
	}
	
	/**
	 * 
	 * @return
	 */
	public PageState firstPage() {
		for(String key : this.getPathKeys()){
			if(key.contains("pagestate")){
				for(PathObject path_obj: this.getPathObjects()){
					if(path_obj.getKey().equals(key) && path_obj.getType().equals("PageState")){
						return (PageState)path_obj;
					}
				}
			}
		}
		return null;
	}

	@Override
	public String generateKey() {
		// TODO Auto-generated method stub
		
	}
}
