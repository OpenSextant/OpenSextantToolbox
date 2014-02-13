package org.opensextant.mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.std.StdDeserializer;

public class AsetDeserializer extends StdDeserializer<Aset> {

  public AsetDeserializer() {
    super(Aset.class);
  }

  @Override
  public Aset deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    ObjectMapper mapper = (ObjectMapper) jp.getCodec();

    Map<String, Object> pieces = mapper.readValue(jp, HashMap.class);

    // pick out the pieces from the Map
    Boolean hasID = (Boolean) pieces.get("hasID");
    if (hasID == null) {
      hasID = false;
    }
    Boolean hasSpan = (Boolean) pieces.get("hasSpan");
    if (hasSpan == null) {
      hasSpan = true;
    }
    String atype = (String) pieces.get("type");
    if (atype == null) {
      atype = "UNKNOWN";
    }
    List<Map<String, String>> attrs = (List<Map<String, String>>) pieces.get("attrs");
    if (attrs == null) {
      attrs = new ArrayList<Map<String, String>>();
    }
    List<List<Object>> annots = (List<List<Object>>) pieces.get("annots");
    if (annots == null) {
      annots = new ArrayList<List<Object>>();
    }

    // create the Aset object
    Aset aset = new Aset();
    aset.setHasID(hasID);
    aset.setHasSpan(hasSpan);
    aset.setType(atype);

    List<Attr> attrs2 = new ArrayList<Attr>();

    for (Map<String, String> m : attrs) {
      Attr tmp = new Attr();
      tmp.setName(m.get("name"));
      tmp.setType(m.get("type"));
      tmp.setAggregation(m.get("aggregration"));
      attrs2.add(tmp);
    }

    aset.setAttrs(attrs2);
    // int attrValueCount = 0;

    for (List<Object> annot : annots) {
      int valuesConsumed = 0;

      Annot tmp = new Annot();
      if (hasSpan && hasID) {
        Integer start = (Integer) annot.get(0);
        Integer end = (Integer) annot.get(1);
        String id = (String) annot.get(2);
        tmp.setStart(new Long(start));
        tmp.setEnd(new Long(end));
        tmp.setId(id);
        valuesConsumed = 3;
      }

      if (hasSpan && !hasID) {
        Integer start = (Integer) annot.get(0);
        Integer end = (Integer) annot.get(1);
        tmp.setStart(new Long(start));
        tmp.setEnd(new Long(end));
        valuesConsumed = 2;
      }

      if (!hasSpan && hasID) {
        String id = (String) annot.get(0);
        tmp.setId(id);
        valuesConsumed = 1;
      }

      if (!hasSpan && !hasID) {
        valuesConsumed = 0;
      }

      // anything left over
      for (int i = valuesConsumed; i < annot.size(); i++) {
        tmp.addAttributeValue(annot.get(i));
      }

      aset.addAnnotation(tmp);
    }

    return aset;
  }
}