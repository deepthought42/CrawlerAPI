package com.qanairy.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditCategory {
	COLOR_MANAGEMENT("color_management"), 
	TYPOGRAPHY("typography"), 
	VISUALS("visuals"), 
	BRANDING("branding"), 
	WRITTEN_CONTENT("written_content"), 
	INFORMATION_ARCHITECTURE("information_architecture"),
	SECURITY("security"),
	UNKNOWN("unknown");
	
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
            if(value.equals(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    public String getShortName() {
        return shortName;
    }
}
