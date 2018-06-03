package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.Attribute;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

public class PageElementSerializer extends StdSerializer<PageElement> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public PageElementSerializer() {
        this(null);
    }
   
    public PageElementSerializer(Class<PageElement> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      PageElement page_element, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", page_element.getKey());
        jgen.writeStringField("name", page_element.getName());
        jgen.writeStringField("screenshot", page_element.getScreenshot());
        jgen.writeStringField("text", page_element.getText());
        jgen.writeStringField("type", page_element.getType());
        jgen.writeStringField("xpath", page_element.getXpath());
        
        jgen.writeArrayFieldStart("attributes");
        for(Attribute attr : page_element.getAttributes()){
        	jgen.writeObject(attr);
        }
        jgen.writeEndArray();
        
        jgen.writeObjectFieldStart("css_values");
        for(String css_key : page_element.getCssValues().keySet()){
        	jgen.writeStringField(css_key, page_element.getCssValues().get(css_key));
        }
        jgen.writeEndObject();
        
        jgen.writeArrayFieldStart("rules");
        for(Rule rule : page_element.getRules()){
        	jgen.writeObject(rule);
        }
        jgen.writeEndArray();

        jgen.writeEndObject();
    }
}
