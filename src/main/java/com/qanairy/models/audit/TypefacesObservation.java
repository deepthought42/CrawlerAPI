package com.qanairy.models.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.qanairy.models.Element;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class TypefacesObservation extends Observation {	
	private List<String> typefaces = new ArrayList<>();
	
	public TypefacesObservation() {}
	
	public TypefacesObservation(
			List<String> typefaces, 
			String description, 
			String why_it_matters, 
			String ada_compliance, 
			Set<String> recommendations, 
			Priority priority) {
		setTypefaces(typefaces);
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setPriority(priority);
		setKey(this.generateKey());
		setRecommendations(recommendations);
	}
	
	/*
	@Override
	public String generateKey() {
		assert typefaces != null;
		String key = typefaces.parallelStream().sorted().collect(Collectors.joining(""));
		
		return "typefaceobservation"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( key + this.getDescription() );
	}
	*/

	public List<String> getTypefaces() {
		return typefaces;
	}


	public void setTypefaces(List<String> typefaces) {
		this.typefaces = typefaces;
	}
	
	public boolean addTypefaces(List<String> typefaces) {
		return this.typefaces.addAll(typefaces);
	}
	

	@Override
	public ObservationType getType() {
		return ObservationType.TYPEFACES;
	}
}
