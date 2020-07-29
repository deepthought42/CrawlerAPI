package com.qanairy.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditCategory {
	COLOR_MANAGEMENT("Color Management"), 
	TYPOGRAPHY("Typography"), 
	VISUALS("Visuals"), 
	BRANDING("Branding"), 
	WRITTEN_CONTENT("Written Content"), 
	INFORMATION_ARCHITECTURE("Information Architecture"),
	SECURITY("Security"),
	UNKNOWN("Unknown");
	
	private String shortName;

    AuditCategory (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AuditCategory create (String value) {
        if(value == null) {
            return UNKNOWN;
        }
        for(AuditCategory v : values()) {
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
