package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.TestRecord;

public class TestRecordSerializer extends StdSerializer<TestRecord> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public TestRecordSerializer() {
        this(null);
    }
   
    public TestRecordSerializer(Class<TestRecord> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      TestRecord test_record, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", test_record.getKey());
        jgen.writeStringField("browser_name", test_record.getBrowser());
        if(test_record.getPassing() != null){
        	jgen.writeBooleanField("passing", test_record.getPassing());
        }
        else{
        	jgen.writeStringField("passing", null);
        }
        jgen.writeNumberField("ran_at", test_record.getRanAt().getTime());
        jgen.writeStringField("result", test_record.getResult().getKey());
        jgen.writeNumberField("run_time", test_record.getRunTime());
        jgen.writeEndObject();
    }
}
