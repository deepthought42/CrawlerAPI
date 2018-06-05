package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.Rule;

public class RuleSerializer extends StdSerializer<Rule> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public RuleSerializer() {
        this(null);
    }
   
    public RuleSerializer(Class<Rule> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      Rule rule, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", rule.getKey());
        jgen.writeStringField("value", rule.getValue());
        jgen.writeStringField("type", rule.getType().toString());
        jgen.writeEndObject();
    }
}
