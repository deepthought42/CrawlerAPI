package com.qanairy.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CrawlAction {
	START_LINK_ONLY("start_link_only"), STOP("stop");
	
	private String shortName;

	CrawlAction (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static CrawlAction create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(CrawlAction v : values()) {
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
