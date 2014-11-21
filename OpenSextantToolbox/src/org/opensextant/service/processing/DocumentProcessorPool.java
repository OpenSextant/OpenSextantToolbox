package org.opensextant.service.processing;

import gate.Annotation;
import gate.AnnotationSet;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Utils;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.opensextant.service.OpenSextantExtractorResource;

public class DocumentProcessorPool {

  private Map<String, BlockingQueue<DocumentProcessor>> poolMap = new HashMap<String, BlockingQueue<DocumentProcessor>>();
  private  long docsProcessedCount =0L;
  private  long docsFailedCount =0L;

  public DocumentProcessorPool(Properties prop) {

    String gateHomeString = prop.getProperty("os.service.gate.home");
    File gateHome = new File(gateHomeString);

    String gappHomeString = prop.getProperty("os.service.gapp.home");
    File gappHome = new File(gappHomeString);

    String[] apps = prop.getProperty("os.service.appnames").split(",");

    Gate.setGateHome(gateHome);
    Gate.setUserConfigFile(new File(gateHome, "user-gate.xml"));
    try {
      Gate.init();
    } catch (GateException e) {
      e.printStackTrace();
    }

    for (String app : apps) {
      String gapp = prop.getProperty("os.service.app." + app + ".gapp");
      int poolSize = Integer.parseInt(prop.getProperty("os.service.app." + app + ".poolsize"));

      File gappFile = new File(gappHome, gapp);

      this.addProcess(app, gappFile, poolSize);

    }

  }

  private void addProcess(String processName, File gappFile, int poolSize) {

    CorpusController template = null;

    try {
      template = (CorpusController) PersistenceManager.loadObjectFromFile(gappFile);
    } catch (PersistenceException e) {
      e.printStackTrace();
    } catch (ResourceInstantiationException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (poolSize > 0) {
      ArrayBlockingQueue<DocumentProcessor> pool = new ArrayBlockingQueue<DocumentProcessor>(poolSize);

      DocumentProcessor dp = new DocumentProcessor(template);
      pool.add(dp);

      for (int i = 0; i < poolSize - 1; i++) {

        try {
          CorpusController tmp = (CorpusController) Factory.duplicate(template);
          DocumentProcessor dpTmp = new DocumentProcessor(tmp);
          pool.add(dpTmp);

        } catch (ResourceInstantiationException e) {
          e.printStackTrace();
        }

      }

      poolMap.put(processName, pool);
    }
  }

  private Document process(String name, Document doc) {

    DocumentProcessor processor = null;

    try {
      processor = poolMap.get(name).take();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    try {
      processor.process(doc);
    } catch (Exception e) {
      docsFailedCount++;
    } finally {
      docsProcessedCount++;
      poolMap.get(name).add(processor);
    }

    return doc;
  }

  public void cleanup() {
    for (String name : poolMap.keySet()) {
      for (DocumentProcessor dp : poolMap.get(name)) {
        dp.cleanup();
      }
    }
  }

  public Set<String> getProcessNames() {
    return poolMap.keySet();
  }

  public Set<String> getResultFormats() {
    return OpenSextantExtractorResource.getFormats();
  }

  public int available(String name) {
    return poolMap.get(name).size();
  }

  public Map<String, Integer> available() {
    Map<String, Integer> avail = new HashMap<String, Integer>();
    for (String name : poolMap.keySet()) {
      avail.put(name, available(name));
    }

    return avail;
  }

  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append("Extractor\tNumber in pool\n");
    for (String name : poolMap.keySet()) {
      buff.append(name + "\t");
      buff.append(poolMap.get(name).size());
      buff.append("\n");
    }
    return buff.toString();
  }

  public DocumentBean process(String extractType, String content) {
    Document gateDoc = null;

    try {
      gateDoc = Factory.newDocument(content);
    } catch (ResourceInstantiationException e) {
      e.printStackTrace();
    }

    return gateDocToBean(process(extractType, gateDoc));
  }

  public DocumentBean process(String extractType, File content) {
    Document gateDoc = null;

    try {
      gateDoc = Factory.newDocument(content.toURI().toURL());
    } catch (ResourceInstantiationException e) {
      e.printStackTrace();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    return gateDocToBean(process(extractType, gateDoc));
  }

  public DocumentBean process(String extractType, URL content) {
    Document gateDoc = null;

    try {
      gateDoc = Factory.newDocument(content);
    } catch (ResourceInstantiationException e) {
      System.err.println("Couldnt create content from " + content.toExternalForm());
      return null;
    }

    return gateDocToBean(process(extractType, gateDoc));
  }

  private DocumentBean gateDocToBean(Document doc) {

    Set<String> featureNameSet = new HashSet<String>();
    featureNameSet.add("isEntity");
    AnnotationSet entitySet = doc.getAnnotations().get(null, featureNameSet);

    DocumentBean db = new DocumentBean();
    db.setContent(doc.getContent().toString());

    for (Annotation a : entitySet) {
      Anno tmpAnno = new Anno(a);

      String type = a.getType();
      tmpAnno.setStart(a.getStartNode().getOffset());
      tmpAnno.setEnd(a.getEndNode().getOffset());
      tmpAnno.setType(type);
      tmpAnno.setMatchText(Utils.cleanStringFor(doc, a));

      if (type.equalsIgnoreCase("PLACE")) {
        FeatureMap fm = a.getFeatures();

        tmpAnno.getFeatures().put("place", fm.get("bestPlace"));
        tmpAnno.getFeatures().put("hierarchy", fm.get("hierarchy"));
        db.addAnno(tmpAnno);
        continue;
      }

      if (type.equalsIgnoreCase("ENTITY")) {
        continue;
      }

      FeatureMap fm = a.getFeatures();
      for (Entry<Object, Object> e : fm.entrySet()) {
        String k = (String) e.getKey();
        Object v = e.getValue();
        tmpAnno.getFeatures().put(k, v);
      }
      db.addAnno(tmpAnno);
    }

    // cleanup resources
    Factory.deleteResource(doc);
    return db;
  }

  /**
   * @return the docsProcessedCount
   */
  public  long getDocsProcessedCount() {
    return docsProcessedCount;
  }

  /**
   * @return the docsFailedCount
   */
  public  long getDocsFailedCount() {
    return docsFailedCount;
  }

}
