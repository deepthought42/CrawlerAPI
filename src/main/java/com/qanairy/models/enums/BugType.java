package com.qanairy.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BugType {
	MISSING_FIELD("MISSING FIELD");
	
	private String shortName;

    BugType (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static BugType create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(BugType v : values()) {
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
