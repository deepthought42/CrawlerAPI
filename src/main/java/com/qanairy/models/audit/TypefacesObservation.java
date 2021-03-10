package com.qanairy.models.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.qanairy.models.Element;
import com.qanairy.models.enums.ObservationType;

/**
 * A observation of potential error for a given {@link Element element} 
 */
public class TypefacesObservation extends Observation {
	private String description;
	
	private List<String> typefaces = new ArrayList<>();
	
	public TypefacesObservation() {}
	
	public TypefacesObservation(List<String> typefaces, String description, String why_it_matters, String ada_compliance) {
		setTypefaces(typefaces);
		setDescription(description);
		setWhyItMatters(why_it_matters);
		setAdaCompliance(ada_compliance);
		setKey(this.generateKey());
	}
	
	@Override
	public String generateKey() {
		assert typefaces != null;
		String key = typefaces.parallelStream().sorted().collect(Collectors.joining(""));
		
		return "observation::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex( key + this.getDescription() );
	}


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
	public String getDescription() {
		return this.description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public ObservationType getType() {
		return ObservationType.TYPEFACES;
	}
}
