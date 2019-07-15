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
public class SlackService {
    
	private static Logger log = LoggerFactory.getLogger(SlackService.class);

    @Value("${slackBotToken}")
    private String slackToken;

    
    public String getSlackToken() {
        return slackToken;
    }
    
    public void sendMessage(String hook_url, String message) throws IOException {
    	
    	// https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX

    	Payload payload = Payload.builder()
    	  .text(message)
    	  .build();

    	Slack slack = Slack.getInstance();
    	Response response = slack.send(hook_url, payload);
    	// response.code, response.message, response.body
    }
}
