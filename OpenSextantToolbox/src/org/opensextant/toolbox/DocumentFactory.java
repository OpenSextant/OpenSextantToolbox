package org.opensextant.toolbox;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.opensextant.mat.ASetSerializer;
import org.opensextant.mat.Annot;
import org.opensextant.mat.Aset;
import org.opensextant.mat.AsetDeserializer;
import org.opensextant.mat.Attr;
import org.opensextant.mat.MATDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentFactory {

  private DocumentFactory() {
  }

  static ObjectMapper m = new ObjectMapper();

  // Log object
  static Logger log = LoggerFactory.getLogger(CRFTaggerTrainerPR.class);

  // create a json doc that contains the specified GATE annotation types
  public static String jsonfromGATEAnnos(Document gateDoc, String inputASName, List<String> types) {

    // create a MAT document and populate it
    MATDocument mat = new MATDocument();

    // set the content
    mat.setSignal(gateDoc.getContent().toString());

    List<Aset> asets = mat.getAsets();
    // set the desired annotations
    for (String aType : types) {

      Aset aset = new Aset();
      aset.setHasSpan(true);
      aset.setHasID(false);
      aset.setType(aType);

      List<Annot> annots = new ArrayList<Annot>();
      aset.setAnnots(annots);
      // aset.setAttrs(attrs); // no attrs, just types for now

      asets.add(aset);

      AnnotationSet tmpSet = gateDoc.getAnnotations(inputASName).get(aType);
      for (Annotation a : tmpSet) {
        Annot matAnno = new Annot();
        // m_anno.setId(id);
        matAnno.setStart(a.getStartNode().getOffset());
        matAnno.setEnd(a.getEndNode().getOffset());
        annots.add(matAnno);
      }

    }

    return jsonfromMATDocument(mat);
  }

  // create a json doc that contains "lex" annotations derived from GATE Token annotations with specified features
  public static String jsonfromGATETokens(Document gateDoc, String inputASName, List<String> featuresToSend) {

    if (featuresToSend == null) {
      featuresToSend = new ArrayList<String>();
    }

    // create a MAT document and populate it
    MATDocument mat = new MATDocument();

    // set the content
    mat.setSignal(gateDoc.getContent().toString());

    // get all the GATE Tokens
    AnnotationSet tokenSet = gateDoc.getAnnotations(inputASName).get("Token");

    // create a MAT Aset for "lex" annotations
    List<Aset> asets = mat.getAsets();
    Aset aset = new Aset();
    aset.setHasSpan(true);
    aset.setHasID(false);
    aset.setType("lex");
    // set the attrs
    List<Attr> attrs = new ArrayList<Attr>();

    // create an attr for each token feature
    for (String s : featuresToSend) {
      Attr tmp = new Attr();
      tmp.setName(s);
      tmp.setType("STRING");
      attrs.add(tmp);
    }

    // add attrs to aset
    aset.setAttrs(attrs);
    // add Aset to document
    asets.add(aset);

    // create a list of MAT annotations
    List<Annot> annots = new ArrayList<Annot>();
    // add to Aset
    aset.setAnnots(annots);

    // convert each GATE token to a MAT "lex"
    for (Annotation a : tokenSet) {
      // get the Token features
      FeatureMap fm = a.getFeatures();
      Annot matAnno = new Annot();
      // m_anno.setId(id);
      matAnno.setStart(a.getStartNode().getOffset());
      matAnno.setEnd(a.getEndNode().getOffset());
      annots.add(matAnno);
      for (String s : featuresToSend) {
        Object value = fm.get(s);
        // if (value == null) // TODO what to do here?, for now just add it
        matAnno.addAttributeValue(value);
      }
    }

    // return as json string
    return jsonfromMATDocument(mat);
  }

  // create a json doc that contains "lex" annotations derived from GATE Token annotations with specified features
  // and contains annotaions of the specified types
  public static String jsonfromGATETokensAndAnnos(Document gateDoc, String inputASName, List<String> featuresToSend,
      List<String> types) {

    if (featuresToSend == null) {
      featuresToSend = new ArrayList<String>();
    }

    if (types == null) {
      types = new ArrayList<String>();
      log.error("No types specificed for training examples");
    }

    // create a MAT document and populate it
    MATDocument mat = new MATDocument();

    // set the content
    mat.setSignal(gateDoc.getContent().toString());

    // get all the GATE Tokens
    AnnotationSet tokenSet = gateDoc.getAnnotations(inputASName).get("Token");

    // create a MAT Aset for "lex" annotations
    List<Aset> asets = mat.getAsets();
    Aset lexSet = new Aset();
    lexSet.setHasSpan(true);
    lexSet.setHasID(false);
    lexSet.setType("lex");
    // set the attrs
    List<Attr> attrs = new ArrayList<Attr>();

    // create an attr for each token feature
    for (String s : featuresToSend) {
      Attr tmp = new Attr();
      tmp.setName(s);
      tmp.setType("STRING");
      attrs.add(tmp);
    }

    // add attrs to aset
    lexSet.setAttrs(attrs);
    // add Aset to document
    asets.add(lexSet);

    // create a list of MAT annotations
    List<Annot> annots = new ArrayList<Annot>();
    // add to Aset
    lexSet.setAnnots(annots);

    // convert each GATE token to a MAT "lex"
    for (Annotation a : tokenSet) {
      // get the Token features
      FeatureMap fm = a.getFeatures();
      Annot matAnno = new Annot();
      // m_anno.setId(id);
      matAnno.setStart(a.getStartNode().getOffset());
      matAnno.setEnd(a.getEndNode().getOffset());
      annots.add(matAnno);
      for (String s : featuresToSend) {
        Object value = fm.get(s);
        // if (value == null) // TODO what to do here?
        matAnno.addAttributeValue(value);
      }
    }

    // set the desired annotations
    for (String aType : types) {

      Aset aset = new Aset();
      aset.setHasSpan(true);
      aset.setHasID(false);
      aset.setType(aType);

      List<Annot> annots2 = new ArrayList<Annot>();
      aset.setAnnots(annots2);
      // aset.setAttrs(attrs); // no attrs, just types for now

      asets.add(aset);

      AnnotationSet tmpSet = gateDoc.getAnnotations(inputASName).get(aType);
      for (Annotation a : tmpSet) {
        Annot matAnno = new Annot();
        // m_anno.setId(id);
        matAnno.setStart(a.getStartNode().getOffset());
        matAnno.setEnd(a.getEndNode().getOffset());
        annots2.add(matAnno);
      }

    }

    // return as json string
    return jsonfromMATDocument(mat);
  }

  // create a json doc using only the content (string) from the GATE document
  public static String jsonfromGATEContent(Document gateDoc) {

    // create a MAT document and populate it
    MATDocument mat = new MATDocument();

    // set the content
    mat.setSignal(gateDoc.getContent().toString());

    // return as json string
    return jsonfromMATDocument(mat);
  }

  // create a MATDocument from a json string
  public static MATDocument matfromJSONString(String json) {

    AsetDeserializer deserializer = new AsetDeserializer();
    SimpleModule module = new SimpleModule("AsetDeserializer", new Version(1, 0, 0, null));
    module.addDeserializer(Aset.class, deserializer);
    m.registerModule(module);

    MATDocument mat = null;
    try {
      mat = m.readValue(json, MATDocument.class);
    } catch (JsonParseException e) {
      log.error("Couldnt parse JSON into MAT document:" + e.getMessage());
    } catch (JsonMappingException e) {
      log.error("Couldnt map JSON into MAT document:" + e.getMessage());
    } catch (IOException e) {
      log.error("IO exception when trying to convert JSON into MAT document:" + e.getMessage());
    }

    return mat;
  }

  // create a json string from a MAT document
  public static String jsonfromMATDocument(MATDocument doc) {

    ASetSerializer serializer = new ASetSerializer(Aset.class);
    SimpleModule module = new SimpleModule("AnnotSerializer", new Version(1, 0, 0, null));
    module.addSerializer(serializer);
    m.registerModule(module);

    String json = null;
    try {
      json = m.writeValueAsString(doc);
    } catch (JsonGenerationException e) {
      log.error("Couldnt generate JSON from MAT document:" + e.getMessage());
    } catch (JsonMappingException e) {
      log.error("Couldnt map JSON from MAT document:" + e.getMessage());
    } catch (IOException e) {
      log.error("IO exception when tryoing to convert MAT document into JSON:" + e.getMessage());
    }

    return json;
  }

  // create a MAT document from a string which is to be its content
  public static MATDocument matfromContentString(String content) {
    MATDocument mat = new MATDocument();
    mat.setSignal(content);
    return mat;
  }

  // TODO is this needed anywhere?
  public static String gatefromMATDocument(MATDocument matDoc) {
    return null;
  }

}
