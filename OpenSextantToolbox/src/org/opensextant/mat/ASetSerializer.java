package org.opensextant.mat;

import java.io.IOException;
import java.lang.reflect.Type;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class ASetSerializer extends SerializerBase<Aset> {

  public ASetSerializer(Class<Aset> t) {
    super(t);
  }

  @Override
  public void serialize(Aset aset, JsonGenerator jgen, SerializerProvider arg2) throws IOException,
      JsonGenerationException {

    jgen.writeStartObject();
    jgen.writeStringField("type", aset.getType());
    jgen.writeBooleanField("hasID", aset.getHasID());
    jgen.writeBooleanField("hasSpan", aset.getHasSpan());

    // attrs
    jgen.writeFieldName("attrs");
    jgen.writeStartArray();
    for (Attr a : aset.getAttrs()) {
      jgen.writeStartObject();
      jgen.writeStringField("name", a.getName());
      jgen.writeStringField("type", a.getType());
      jgen.writeStringField("aggregation", a.getAggregation());
      jgen.writeEndObject();
    }
    jgen.writeEndArray();

    // annots
    jgen.writeFieldName("annots");
    jgen.writeStartArray();
    for (Annot an : aset.getAnnots()) {
      jgen.writeStartArray();
      if (aset.getHasSpan()) {
        jgen.writeNumber(an.getStart());
        jgen.writeNumber(an.getEnd());
      }

      if (aset.getHasID()) {
        jgen.writeString(an.getId());
      }

      for (Object o : an.getAttributeValues()) {
        jgen.writeObject(o);
      }

      jgen.writeEndArray();
    }

    jgen.writeEndArray();

    jgen.writeEndObject();

  }

  @Override
  public JsonNode getSchema(SerializerProvider arg0, Type arg1) throws JsonMappingException {
    // always return null?;
    return null;
  }

}
