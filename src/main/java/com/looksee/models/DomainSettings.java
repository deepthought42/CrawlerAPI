package com.looksee.models;

import java.util.UUID;

/**
 * The settings that the business uses to define the brand guidelines 
 * 	and design systems customer identification
 */
public class DomainSettings extends LookseeObject{
	private String expertise;
	private String tone_of_voice;
	
	public DomainSettings(String expertise_level, String tone_of_voice) {
		setExpertise(expertise_level);
		setToneOfVoice(tone_of_voice);
	}

	public String getExpertise() {
		return expertise;
	}

	public void setExpertise(String expertise_level) {
		this.expertise = expertise_level;
	}

	public String getToneOfVoice() {
		return tone_of_voice;
	}

	public void setToneOfVoice(String tone_of_voice) {
		this.tone_of_voice = tone_of_voice;
	}

	@Override
	public String generateKey() {
		return "domainsetting:"+UUID.randomUUID();
	}
}
