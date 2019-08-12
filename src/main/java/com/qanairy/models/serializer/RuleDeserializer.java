package com.qanairy.models.serializer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.qanairy.models.rules.Rule;
import com.qanairy.models.rules.RuleFactory;

public class RuleDeserializer extends JsonDeserializer<Rule> {
	private static Logger log = LoggerFactory.getLogger(RuleDeserializer.class);

    public RuleDeserializer() {
        this(null);
    }
   
    public RuleDeserializer(Class<Rule> t) {
        super();
    }
 
    @Override
    public Rule deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    	 ObjectCodec oc = jp.getCodec();
    	    JsonNode node = oc.readTree(jp);
    	    final String type = node.get("type").asText();
    	    final String value = node.get("value").asText();
    	    log.warn("type :: "+type);
    	    log.warn("value  ::   " + value);
    	    //Rule user = new Rule();
    	    //user.setId(ownerId);
    	    //return new Rule(id, name, contents, null);
    	    return RuleFactory.build(type, value);
    }
}