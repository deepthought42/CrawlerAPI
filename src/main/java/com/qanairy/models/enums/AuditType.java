package com.qanairy.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link AuditDetail} that exist in the system
 */
public enum AuditType {
	TABLE("table"), FILMSTRIP("filmstrip"), OPPORTUNITY("opportunity");
	
	private String shortName;

    AuditType (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AuditType create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(AuditType v : values()) {
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
