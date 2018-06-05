package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.Attribute;

public class AttributeSerializer extends StdSerializer<Attribute> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public AttributeSerializer() {
        this(null);
    }
   
    public AttributeSerializer(Class<Attribute> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      Attribute attribute, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", attribute.getKey());
        jgen.writeStringField("name", attribute.getName());
        
        jgen.writeArrayFieldStart("values");
        for(String val : attribute.getVals()){
        	jgen.writeString(val);
        }
        jgen.writeEndArray();
        
        jgen.writeEndObject();
    }
}
