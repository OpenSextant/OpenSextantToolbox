package org.opensextant.tagger.gate;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.opensextant.tagger.Document;
import org.opensextant.tagger.Match;
import org.opensextant.tagger.Tagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CorpusController;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Utils;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;

public class GATETagger implements Tagger {

	private CorpusController controller;
	private Corpus corpus;
	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(GATETagger.class);

	String taggerType = "";

	public GATETagger(String type, Properties prop) {

		String gateHomeString = prop.getProperty("os.service.gate.home");
		File gateHome = new File(gateHomeString);

		String gappHomeString = prop.getProperty("os.service.gapp.home");
		File gappHome = new File(gappHomeString);
		LOGGER.debug("Trying Gate Home  " + gateHome.getAbsolutePath());
		LOGGER.debug("Trying GAPP Home  " + gappHome.getAbsolutePath());

		if (!Gate.isInitialised()) {
			Gate.setGateHome(gateHome);
			Gate.setUserConfigFile(new File(gateHome, "user-gate.xml"));
			try {
				Gate.init();
				LOGGER.debug("Gate Home is " + Gate.getGateHome());
				LOGGER.debug("Plugins Home is " + Gate.getPluginsHome());
			} catch (GateException e) {
				LOGGER.error("Couldn't init GATE", e);
			}
		}

		String solrHome = prop.getProperty("os.service.solr.home");
		System.setProperty("solr.home", solrHome);

		taggerType = type;
		String gapp = prop.getProperty("os.service.app." + type + ".gapp");

		File gappFile = new File(gappHome, gapp);

		try {
			this.controller = (CorpusController) PersistenceManager.loadObjectFromFile(gappFile);
		} catch (PersistenceException e) {
			LOGGER.error("Couldn't load GAPP file" + gappFile.getName(), e);
		} catch (ResourceInstantiationException e) {
			LOGGER.error("Couldn't load GAPP file" + gappFile.getName(), e);
		} catch (IOException e) {
			LOGGER.error("Couldn't load GAPP file" + gappFile.getName(), e);
		}

	}

	@Override
	public Document tag(String content) {
		gate.Document gateDoc = null;

		try {
			gateDoc = Factory.newDocument(content);
		} catch (ResourceInstantiationException e) {
			LOGGER.error("Couldn't create new document from given string", e);
		}

		return process(gateDoc);
	}

	@Override
	public Document tag(File content) {
		gate.Document gateDoc = null;

		try {
			gateDoc = Factory.newDocument(content.toURI().toURL());
		} catch (ResourceInstantiationException e) {
			LOGGER.error("Couldn't create new document from " + content.getName(), e);
		} catch (MalformedURLException e) {
			LOGGER.error("Couldn't create new document from " + content.getName(), e);
		}

		return process(gateDoc);
	}

	@Override
	public Document tag(URL content) {
		gate.Document gateDoc = null;

		try {
			gateDoc = Factory.newDocument(content);
		} catch (ResourceInstantiationException e) {
			LOGGER.error("Couldnt create content from given URL", e);
			return null;
		}

		return process(gateDoc);
	}

	@Override
	public List<Match> match(String content) {
		return tag(content).getMatchList();
	}

	@Override
	public List<Match> match(File content) {
		return tag(content).getMatchList();
	}

	@Override
	public List<Match> match(URL content) {
		return tag(content).getMatchList();
	}

	@Override
	public synchronized void cleanup() {
		Factory.deleteResource(controller);
		if (corpus != null) {
			Factory.deleteResource(corpus);
		}
	}

	@Override
	public String getTaggerType() {
		return this.taggerType;
	}

	private Document process(gate.Document doc) {

		if (corpus == null) {

			try {
				corpus = Factory.newCorpus("DP Corpus");
			} catch (ResourceInstantiationException e) {
				LOGGER.error("Couldnt create new corpus", e);
			}
		}

		try {
			corpus.add(doc);
			controller.setCorpus(corpus);

			try {
				controller.execute();
			} catch (ExecutionException e) {
				LOGGER.error("Couldnt execute document processing", e);
			}

		} finally {
			controller.setCorpus(null);
			corpus.clear();
		}

		return gateDocToDocument(doc);
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

			Match match = new Match();
			match.setStart(a.getStartNode().getOffset());
			match.setEnd(a.getEndNode().getOffset());
			match.setType(type);
			match.setMatchText(Utils.cleanStringFor(doc, a));

			Map<String, Object> feature = new HashMap<String, Object>();

			FeatureMap fm = a.getFeatures();

			/* special handling for PLACEs */
			if ("PLACE".equalsIgnoreCase(type)) {
				feature.put("place", fm.get("bestPlace"));
				feature.put("hierarchy", fm.get("hierarchy"));
			} else {
				for (Entry<Object, Object> e : fm.entrySet()) {
					String k = (String) e.getKey();
					Object v = e.getValue();
					feature.put(k, v);
				}
			}

			match.addPayload(feature);

			db.addMatch(match);

		}

		// cleanup resources
		Factory.deleteResource(doc);
		return db;
	}

}
