package com.qanairy.models.audit;

import java.util.Set;

import com.qanairy.models.SimpleElement;

public class ElementObservationMap {
	private Set<Observation> observations;
	private SimpleElement element;

	
	public ElementObservationMap(
			Set<Observation> observation,
			SimpleElement elements
	) {
		setObservations(observation);
		setElement(elements);
	}


	public Set<Observation> getObservations() {
		return observations;
	}

	public void setObservations(Set<Observation> observations) {
		this.observations = observations;
	}

	public SimpleElement getElement() {
		return element;
	}

	public void setElement(SimpleElement element) {
		this.element = element;
	}
}
