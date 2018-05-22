package com.qanairy.persistence.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.persistence.Domain;

public class DomainSerializer extends StdSerializer<Domain> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public DomainSerializer() {
        this(null);
    }
   
    public DomainSerializer(Class<Domain> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      Domain domain, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", domain.getKey());
        jgen.writeStringField("browser_name", domain.getDiscoveryBrowserName());
        jgen.writeStringField("logo_url", domain.getLogoUrl());
        jgen.writeStringField("protocol", domain.getProtocol());
        jgen.writeStringField("url", domain.getUrl());

        jgen.writeEndObject();
    }
}
