package com.looksee.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubUrlMessagePublisherImpl extends PubSubPublisher {

    @SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(PubSubUrlMessagePublisherImpl.class);

    @Value("${pubsub.url_topic}")
    private String topic;
    
    @Override
    protected String topic() {
        return this.topic;
    }
}