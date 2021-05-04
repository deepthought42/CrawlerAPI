package com.looksee.models.journeys;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.looksee.models.LookseeObject;

/**
 * Represents the series of steps taken for an end to end journey
 */
public class Journey extends LookseeObject {

	private Set<Step> steps;
	private List<String> ordered_keys;
	
	public Journey() {
		setSteps(new HashSet<>());
		setOrderedKeys(new ArrayList<>());
		setKey(generateKey());
	}
	
	public Journey(Set<Step> steps, List<String> ordered_keys) {
		setSteps(steps);
		setOrderedKeys(ordered_keys);
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(StringUtils.join(ordered_keys, "|"));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Journey clone() {
		return new Journey(new HashSet<>(getSteps()), new ArrayList<>(getOrderedKeys()));
	}
	
	public Set<Step> getSteps() {
		return steps;
	}

	public void setSteps(Set<Step> steps) {
		this.steps = steps;
	}

	public boolean addStep(Step step) {
		return this.steps.add(step);
	}
	
	public List<String> getOrderedKeys() {
		return ordered_keys;
	}
	
	public void setOrderedKeys(List<String> ordered_keys) {
		this.ordered_keys = ordered_keys;
	}
}
