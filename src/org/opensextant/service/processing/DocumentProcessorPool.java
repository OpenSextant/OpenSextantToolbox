package org.opensextant.service.processing;

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
import org.opensextant.tagger.Document;
import org.opensextant.tagger.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gate.Annotation;
import gate.AnnotationSet;
import gate.CorpusController;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Utils;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;

public class DocumentProcessorPool {

	private Map<String, BlockingQueue<DocumentProcessor>> poolMap = new HashMap<String, BlockingQueue<DocumentProcessor>>();
	private long docsProcessedCount;
	private long docsFailedCount;

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentProcessorPool.class);

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
			LOGGER.error("Couldn't init GATE", e);
		}

		for (String app : apps) {
			String gapp = prop.getProperty("os.service.app." + app + ".gapp");
			int poolSize = Integer.parseInt(prop.getProperty("os.service.app." + app + ".poolsize"));

			File gappFile = new File(gappHome, gapp);

			addProcess(app, gappFile, poolSize);

		}

	}

	private void addProcess(String processName, File gappFile, int poolSize) {

		CorpusController template = null;

		try {
			template = (CorpusController) PersistenceManager.loadObjectFromFile(gappFile);
		} catch (PersistenceException e) {
			LOGGER.error("Couldn't load GAPP file" + gappFile.getName(), e);
		} catch (ResourceInstantiationException e) {
			LOGGER.error("Couldn't load GAPP file" + gappFile.getName(), e);
		} catch (IOException e) {
			LOGGER.error("Couldn't load GAPP file" + gappFile.getName(), e);
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
					LOGGER.error("Couldn't create controller for " + gappFile.getName(), e);
				}

			}

			poolMap.put(processName, pool);
		}
	}

	private gate.Document process(String name, gate.Document doc) {

		DocumentProcessor processor = null;

		try {
			processor = poolMap.get(name).take();
		} catch (InterruptedException e) {
			LOGGER.error("Couldn't get a processor from the pool", e);
		}

		if (processor == null) {
			LOGGER.error("Couldn't get a processor from the pool");
			return doc;
		}

		try {
			processor.process(doc);
		} catch (Exception e) {
			docsFailedCount++;
			LOGGER.error("Document failed to process" + doc.getName(), e);
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

	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		buff.append("Extractor\tNumber in pool\n");
		for (String name : poolMap.keySet()) {
			buff.append(name).append("\t");
			buff.append(poolMap.get(name).size());
			buff.append("\n");
		}
		return buff.toString();
	}

	public Document process(String extractType, String content) {
		gate.Document gateDoc = null;

		try {
			gateDoc = Factory.newDocument(content);
		} catch (ResourceInstantiationException e) {
			LOGGER.error("Couldn't create new document from given string", e);
		}

		return gateDocToDocument(process(extractType, gateDoc));
	}

	public Document process(String extractType, File content) {
		gate.Document gateDoc = null;

		try {
			gateDoc = Factory.newDocument(content.toURI().toURL());
		} catch (ResourceInstantiationException e) {
			LOGGER.error("Couldn't create new document from " + content.getName(), e);
		} catch (MalformedURLException e) {
			LOGGER.error("Couldn't create new document from " + content.getName(), e);
		}

		return gateDocToDocument(process(extractType, gateDoc));
	}

	public Document process(String extractType, URL content) {
		gate.Document gateDoc = null;

		try {
			gateDoc = Factory.newDocument(content);
		} catch (ResourceInstantiationException e) {
			LOGGER.error("Couldnt create content from given URL", e);
			return null;
		}

		return gateDocToDocument(process(extractType, gateDoc));
	}

	private Document gateDocToDocument(gate.Document doc) {

		Set<String> featureNameSet = new HashSet<String>();
		featureNameSet.add("isEntity");
		AnnotationSet entitySet = doc.getAnnotations().get(null, featureNameSet);

		Document db = new Document();
		db.setContent(doc.getContent().toString());

		for (Annotation a : entitySet) {

			String type = a.getType();
			if ("ENTITY".equalsIgnoreCase(type)) {
				continue;
			}

			Match tmpAnno = new Match();
			tmpAnno.setStart(a.getStartNode().getOffset());
			tmpAnno.setEnd(a.getEndNode().getOffset());
			tmpAnno.setType(type);
			tmpAnno.setMatchText(Utils.cleanStringFor(doc, a));

			FeatureMap fm = a.getFeatures();
			/* special handling for PLACEs */
			if ("PLACE".equalsIgnoreCase(type)) {
				tmpAnno.getFeatures().put("place", fm.get("bestPlace"));
				tmpAnno.getFeatures().put("hierarchy", fm.get("hierarchy"));
			} else {
				for (Entry<Object, Object> e : fm.entrySet()) {
					String k = (String) e.getKey();
					Object v = e.getValue();
					tmpAnno.getFeatures().put(k, v);
				}
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
	public long getDocsProcessedCount() {
		return docsProcessedCount;
	}

	/**
	 * @return the docsFailedCount
	 */
	public long getDocsFailedCount() {
		return docsFailedCount;
	}

}
