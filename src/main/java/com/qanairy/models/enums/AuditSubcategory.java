package com.qanairy.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditSubcategory {
	//color management
	COLOR_PALETTE("color_palette"),
	TEXT_BACKGROUND_CONTRAST("text_background_contrast"),
	NON_TEXT_BACKGROUND_CONTRAST("non_text_background_contrast"),
	LINKS("link"),
	UNKNOWN("unknown");
	
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
        if(value == null) {
            return UNKNOWN;
        }
        for(AuditSubcategory v : values()) {
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
