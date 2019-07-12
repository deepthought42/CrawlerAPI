package com.qanairy.integrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import me.ramswaroop.jbot.core.slack.Bot;
import me.ramswaroop.jbot.core.slack.models.Attachment;
import me.ramswaroop.jbot.core.slack.models.Event;
import me.ramswaroop.jbot.core.slack.models.Message;
import me.ramswaroop.jbot.core.slack.models.RichMessage;


@Component
public class SlackBot extends Bot {
    
	private static Logger log = LoggerFactory.getLogger(SlackBot.class);

    @Value("${slackBotToken}")
    private String slackToken;

    @Override
    public String getSlackToken() {
        return slackToken;
    }

    @Override
    public Bot getSlackBot() {
        return this;
    }
    
    public void sendMessage(String channel_id, String message) {
        /** build response */
        RichMessage richMessage = new RichMessage("The is Slash Commander!");
        richMessage.setResponseType("in_channel");
        // set attachments
        richMessage.setChannel(channel_id);
        richMessage.setText(message);
        sendMessage(channel_id, message);
    }
}
