package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.Action;

public class ActionSerializer extends StdSerializer<Action> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public ActionSerializer() {
        this(null);
    }
   
    public ActionSerializer(Class<Action> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      Action action, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", action.getKey());
        jgen.writeStringField("name", action.getName());
        jgen.writeStringField("type", action.getType());
        jgen.writeStringField("value", action.getValue());

        jgen.writeEndObject();
    }
}
