package com.qanairy.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.models.PageState;
import com.qanairy.models.ScreenshotSet;

public class PageStateSerializer  extends StdSerializer<PageState> {
    
    public PageStateSerializer() {
        this(null);
    }
   
    public PageStateSerializer(Class<PageState> t) {
        super(t);
    }
 
    @Override
    public void serialize(
    		PageState page_state, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", page_state.getKey());
        jgen.writeStringField("type", page_state.getType());
        jgen.writeStringField("url", page_state.getUrl());
        jgen.writeArrayFieldStart("browserScreenshots");
        
        for(ScreenshotSet screenshot : page_state.getBrowserScreenshots()){
        	jgen.writeObject(screenshot);
        }
        jgen.writeEndArray();
        jgen.writeEndObject();
    }
}