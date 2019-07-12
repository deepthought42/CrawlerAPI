package com.qanairy.integrations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hubspot.algebra.Result;
import com.hubspot.slack.client.SlackClient;
import com.hubspot.slack.client.methods.params.chat.ChatPostMessageParams;
import com.hubspot.slack.client.models.response.SlackError;
import com.hubspot.slack.client.models.response.chat.ChatPostMessageResponse;

@Component
public class SlackBot {
    
	@Autowired
	private SlackClient client;
	
	 public static ChatPostMessageResponse messageChannel(String channelToPostIn, SlackClient slackClient) {
		    Result<ChatPostMessageResponse, SlackError> postResult = slackClient.postMessage(
		        ChatPostMessageParams.builder()
		            .setText("Hello me! Here's a slack message!")
		            .setChannelId(channelToPostIn)
		            .build()
		    ).join();

		    return postResult.unwrapOrElseThrow(); // release failure here as a RTE
		  }
}
