package com.looksee.models.enums;

public enum SubscriptionPlan {
	FREE("FREE"), PRO("PRO"), ENTERPRISE("ENTERPRISE"), STARTUP("STARTUP"), COMPANY_BASIC("COMPANY_BASIC"),
	COMPANY_PRO("COMPANY_PRO"), AGENCY("AGENCY"), AGENCY_BASIC("AGENCY_BASIC"), AGENCY_PRO("AGENCY_PRO"),
	UNLIMITED("UNLIMITED");


    private final String text;

    /**
     * @param text
     */
    private SubscriptionPlan(final String text) {
        this.text = text;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }
}
