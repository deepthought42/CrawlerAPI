package com.qanairy.models.audit;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.Element;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.enums.ObservationType;

/**
 * A observation of potential error for a given {@link Element element} 
 */
@NodeEntity
public class Observation extends LookseeObject{
	private String description;
	private String type;
	private String why_it_matters;
	private String ada_compliance;
	
	@Relationship(type = "HAS")
	private Set<UXIssueMessage> message;	
	
	// labels are intended to contain things like subcategory, accessibility, etc
	private Set<String> labels;
	private Set<String> categories;
	
	public Observation() {
		setMessages(new HashSet<>());
	}
	
	public Observation(
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			ObservationType type, 
			Set<String> labels,
			Set<String> categories, 
			Set<UXIssueMessage> ux_issue_message
	) {
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setType(type);
		setLabels(labels);
		setCategories(categories);
		setMessages(ux_issue_message);
		setKey(generateKey());
	}
	
	public Observation(
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			String key,
			ObservationType type, 
			Set<String> labels,
			Set<String> categories, 
			Set<UXIssueMessage> ux_issue_messages
	) {
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setType(type);
		setLabels(labels);
		setCategories(categories);
		setMessages(ux_issue_messages);
		setKey(key);
	}
	
	@Override
	public String generateKey() {
		return "observation"+getSaltString();
	}
	
	protected String getSaltString() {
        String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 32) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }
	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setType(ObservationType type) {
		this.type = type.getShortName();
	}
	
	public ObservationType getType() {
		return ObservationType.create(this.type);
	}
	
	public String getWhyItMatters() {
		return why_it_matters;
	}

	public void setWhyItMatters(String why_it_matters) {
		this.why_it_matters = why_it_matters;
	}

	public String getAdaCompliance() {
		return ada_compliance;
	}

	public void setAdaCompliance(String ada_compliance) {
		this.ada_compliance = ada_compliance;
	}


	public Set<String> getLabels() {
		return labels;
	}

	public void setLabels(Set<String> labels) {
		this.labels = labels;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}

	public Set<UXIssueMessage> getMessages() {
		return message;
	}

	public void setMessages(Set<UXIssueMessage> message) {
		this.message = message;
	}
}
