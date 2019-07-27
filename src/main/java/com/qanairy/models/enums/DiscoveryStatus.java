package com.qanairy.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DiscoveryStatus {
	RUNNING("running"), STOPPED("stopped"), COMPLETE("complete");
	
	private String shortName;

    DiscoveryStatus (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static DiscoveryStatus creat(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(DiscoveryStatus v : values()) {
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
