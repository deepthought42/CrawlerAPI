package com.qanairy.integrations;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.seratch.jslack.*;
import com.github.seratch.jslack.api.webhook.*;

import okhttp3.Response;


@Component
public class SlackBot {
    
	private static Logger log = LoggerFactory.getLogger(SlackBot.class);

    @Value("${slackBotToken}")
    private String slackToken;

    
    public String getSlackToken() {
        return slackToken;
    }
    
    public void sendMessage(String channel_id, String message) throws IOException {
    	// https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX
    	String url = System.getenv("SLACK_WEBHOOK_URL");

    	Payload payload = Payload.builder()
    	  .text(message)
    	  .build();

    	Slack slack = Slack.getInstance();
    	Response response = slack.send(url, payload);
    	// response.code, response.message, response.body
    }
}
