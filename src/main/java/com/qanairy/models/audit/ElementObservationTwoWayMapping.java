package com.qanairy.models.audit;

import java.util.Set;

public class ElementObservationTwoWayMapping {
	private Set<ObservationElementMap> observation_elements;
	private Set<ElementObservationMap> element_observations;

	
	public ElementObservationTwoWayMapping(
			Set<ObservationElementMap> observation_elements,
			Set<ElementObservationMap> element_observations
	) {
		setObservationElements(observation_elements);
		setElementObservations(element_observations);
	}


	public Set<ObservationElementMap> getObservationElements() {
		return observation_elements;
	}


	public void setObservationElements(Set<ObservationElementMap> observation_elements) {
		this.observation_elements = observation_elements;
	}


	public Set<ElementObservationMap> getElementObservations() {
		return element_observations;
	}


	public void setElementObservations(Set<ElementObservationMap> element_observations) {
		this.element_observations = element_observations;
	}

}
