package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestRecord;

/**
 * 
 */
public class TestSerializer extends StdSerializer<Test> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public TestSerializer() {
        this(null);
    }
   
    public TestSerializer(Class<Test> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      Test test, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", test.getKey());
        jgen.writeStringField("name", test.getName());
        jgen.writeNumberField("run_time", test.getRunTime());
        //jgen.writeNumberField("browser_statuses", test.getBrowserStatuses());
        jgen.writeStartObject();
        jgen.writeObjectFieldStart("allInfo");
        for(String key : test.getBrowserStatuses().keySet()){
	        jgen.writeObjectField(key, test.getBrowserStatuses().get(key));
	        jgen.writeEndObject();
    	}        
        jgen.writeBooleanField("correct", test.getCorrect());
        jgen.writeNumberField("last_run_timestamp", test.getLastRunTimestamp().getTime());

        jgen.writeArrayFieldStart("path_keys");
        jgen.writeStartArray();
        for (String key: test.getPathKeys()) {
        	jgen.writeString(key);
        }
        jgen.writeEndArray();
        
        jgen.writeArrayFieldStart("path_objects");
        jgen.writeStartArray();
        for (PathObject path_obj: test.getPathObjects()) {
        	jgen.writeObject(path_obj);
        }
        jgen.writeEndArray();
        

        jgen.writeArrayFieldStart("records");
        jgen.writeStartArray();
        for (TestRecord test_record: test.getRecords()) {
        	jgen.writeObject(test_record);
        }
        jgen.writeEndArray();
        
        //jgen.writeStringField("test_records", test.getRecords());
        
        jgen.writeStartObject();
        jgen.writeObjectFieldStart("result");
        jgen.writeObject(test.getResult());
        jgen.writeEndObject();
        
        //jgen.writeStringField("result", test.getResult());
        jgen.writeBooleanField("spans_multiple_domains", test.getSpansMultipleDomains());

        jgen.writeEndObject();
    }
}
