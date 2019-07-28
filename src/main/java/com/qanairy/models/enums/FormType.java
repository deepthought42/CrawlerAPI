package com.qanairy.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FormType {
	LOGIN("login"), REGISTRATION("registration"), CONTACT_COMPANY("contact_company"), SUBSCRIBE("subscribe"), 
	LEAD("lead"), SEARCH("search"), PASSWORD_RESET("password_reset"), PAYMENT("payment"), UNKNOWN("unknown");
	
	private String shortName;

    FormType(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static FormType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(FormType v : values()) {
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
