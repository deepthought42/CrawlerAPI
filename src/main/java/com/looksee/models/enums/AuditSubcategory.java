package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditSubcategory {
	//color management
	WRITTEN_CONTENT("Written Content"),
	IMAGERY("Imagery"),
	VIDEOS("Videos"),
	AUDIO("Audio"),
	MENU_ANALYSIS("Menu Analysis"),
	PERFORMANCE("Performance"),
	SEO("SEO"),
	TYPOGRAPHY("TYPOGRAPHY"),
	COLOR_MANAGEMENT("Color Management"),
	WHITESPACE("Whitespace"),
	BRANDING("Branding");
	
	private String shortName;

    AuditSubcategory (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AuditSubcategory create (String value) {

        for(AuditSubcategory v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    public String getShortName() {
        return shortName;
    }
}
