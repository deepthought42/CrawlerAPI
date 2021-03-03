package com.qanairy.models.audit;

import java.util.Set;

import com.qanairy.models.SimpleElement;

public class ObservationElementMap {
	private Observation observation;
	private Set<SimpleElement> elements;

	
	public ObservationElementMap(
			Observation observation,
			Set<SimpleElement> elements
	) {
		setObservation(observation);
		setElements(elements);
	}


	public Observation getObservation() {
		return observation;
	}

	public void setObservation(Observation observation) {
		this.observation = observation;
	}

	public Set<SimpleElement> getElements() {
		return elements;
	}

	public void setElements(Set<SimpleElement> elements) {
		this.elements = elements;
	}
}
