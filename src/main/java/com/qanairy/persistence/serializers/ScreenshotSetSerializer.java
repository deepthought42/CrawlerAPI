package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.models.ScreenshotSet;

public class ScreenshotSetSerializer extends StdSerializer<ScreenshotSet> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public ScreenshotSetSerializer() {
        this(null);
    }
   
    public ScreenshotSetSerializer(Class<ScreenshotSet> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      ScreenshotSet screenshot_set, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", screenshot_set.getKey());
        jgen.writeStringField("browser_name", screenshot_set.getBrowser());
        jgen.writeStringField("full_screenshot", screenshot_set.getFullScreenshot());
        jgen.writeStringField("viewport_screenshot", screenshot_set.getViewportScreenshot());

        jgen.writeEndObject();
    }
}
