package com.looksee.models.enums;

public enum SubscriptionPlan {
	FREE("FREE"), PRO("PRO"), ENTERPRISE("ENTERPRISE");


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
