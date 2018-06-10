package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.DiscoveryRecord;

public class DiscoveryRecordSerializer extends StdSerializer<DiscoveryRecord> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public DiscoveryRecordSerializer() {
        this(null);
    }
   
    public DiscoveryRecordSerializer(Class<DiscoveryRecord> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      DiscoveryRecord record, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", record.getKey());
        jgen.writeStringField("browser_name", record.getBrowserName());
        jgen.writeNumberField("last_ran_at", record.getLastPathRanAt().getTime());
        jgen.writeNumberField("start_time", record.getStartTime().getTime());
        jgen.writeNumberField("test_count", record.getTestCount());
        jgen.writeNumberField("path_count", record.getTotalPathCount());
        jgen.writeNumberField("examined_path_count", record.getExaminedPathCount());
        jgen.writeNumberField("total_path_count", record.getTotalPathCount());
        jgen.writeStringField("url", record.getDomainUrl());

        jgen.writeEndObject();
    }
}
