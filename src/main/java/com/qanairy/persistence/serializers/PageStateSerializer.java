package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.ScreenshotSet;

public class PageStateSerializer  extends StdSerializer<PageState> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

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
         //jgen.writeStringField("src", page_state.getSrc());
         
         jgen.writeArrayFieldStart("screenshot_set");
         for(ScreenshotSet screenshot : page_state.getBrowserScreenshots()){
             jgen.writeObject(screenshot);
         }
         jgen.writeEndArray();
         
         /*
         jgen.writeArrayFieldStart("page_elements");
         for(PageElement element: page_state.getElements()){
             jgen.writeObject(element);
         }
         jgen.writeEndArray();
         */
         
         if(page_state.getTotalWeight() != null){
             jgen.writeNumberField("total_weight", page_state.getTotalWeight());
         }
         
         jgen.writeStringField("url", page_state.getUrl().toString());
         jgen.writeStringField("type", page_state.getType());

         jgen.writeEndObject();
    }

}
