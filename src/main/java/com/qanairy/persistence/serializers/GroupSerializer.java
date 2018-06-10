package com.qanairy.persistence.serializers;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.qanairy.models.Group;

public class GroupSerializer extends StdSerializer<Group> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1782324084563346L;

	public GroupSerializer() {
        this(null);
    }
   
    public GroupSerializer(Class<Group> t) {
        super(t);
    }
    
    @Override
    public void serialize(
      Group group, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        jgen.writeStringField("key", group.getKey());
        jgen.writeStringField("name", group.getName());
        jgen.writeStringField("description", group.getDescription());

        jgen.writeEndObject();
    }
}
