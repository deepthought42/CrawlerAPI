package com.looksee.models.message;

public class ExceededSubscriptionMessage extends Message{

	
	public ExceededSubscriptionMessage(long accountId) {
		setAccountId(accountId);
	}
	
}
