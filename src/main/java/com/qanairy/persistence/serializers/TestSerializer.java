package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.Group;
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
    	jgen.writeNumberField("last_run_timestamp", test.getLastRunTimestamp().getTime());
        jgen.writeNumberField("run_time", test.getRunTime());
        if(test.getCorrect() != null){
        	jgen.writeBooleanField("correct", test.getCorrect().booleanValue());
        }
        
        jgen.writeObjectFieldStart("browser_statuses");
        for(String key : test.getBrowserStatuses().keySet()){
        	jgen.writeObjectField(key, test.getBrowserStatuses().get(key));
	        //jgen.writeEndObject();
    	}        
        jgen.writeEndObject();
        

        jgen.writeArrayFieldStart("path_keys");
        for (String key: test.getPathKeys()) {
        	jgen.writeString(key);
        }
        jgen.writeEndArray();

        System.err.println("test groups size :: "+test.getGroups());
        jgen.writeArrayFieldStart("groups");
        for (Group group: test.getGroups()) {
        	System.err.println("test group :: "+group);
        	jgen.writeObject(group);
        }
        jgen.writeEndArray();
                
        
        jgen.writeArrayFieldStart("records");
        for (TestRecord test_record: test.getRecords()) {
        	jgen.writeObject(test_record);
        }
        jgen.writeEndArray();
                
        jgen.writeObjectField("result", test.getResult());        
        jgen.writeBooleanField("spans_multiple_domains", test.getSpansMultipleDomains());

        jgen.writeEndObject();
    }
}
