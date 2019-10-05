package com.qanairy.analytics;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.qanairy.models.Account;
import com.qanairy.models.Test;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;

@Service
public class SegmentAnalyticsHelper {

	private static Analytics analytics = Analytics.builder("2fhUNnmoIEy5DZj9yhysv9j7QQcgWQlT").build();
	
	public static void sendTestFinishedRunningEvent(String userId, Test test) {
		Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("testId", test.getKey());
        final String anonymousId = UUID.randomUUID().toString();

		analytics.enqueue(
	            TrackMessage.builder("Test Finished Running")
	                .properties(properties)
	                .anonymousId(anonymousId)
	                .userId(userId));
	}

	public static void testRunStarted(String userId, int test_count) {
		//Fire test run started event
	   	Map<String, String> run_test_batch_props= new HashMap<String, String>();
	   	run_test_batch_props.put("total tests", Integer.toString(test_count));
	   	analytics.enqueue(TrackMessage.builder("Running tests")
   		    .userId(userId)
   		    .properties(run_test_batch_props)
   		);
	}

	public static void signupEvent(String userId, String string) {
		Map<String, String> account_signup_properties = new HashMap<String, String>();
    	account_signup_properties.put("plan", "FREE");
    	analytics.enqueue(TrackMessage.builder("Signed Up")
    		    .userId(userId)
    		    .properties(account_signup_properties)
    		);
	}

	public static void identify(Account acct) {
		Map<String, String> traits = new HashMap<String, String>();
        traits.put("email", acct.getUsername());
    	analytics.enqueue(IdentifyMessage.builder()
    		    .userId(acct.getUserId())
    		    .traits(traits)
    		);
	}
}
