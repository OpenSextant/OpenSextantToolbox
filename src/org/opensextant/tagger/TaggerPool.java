package org.opensextant.tagger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.opensextant.tagger.gate.GATETagger;
import org.opensextant.tagger.regex.RegexTagger;
import org.opensextant.tagger.service.OpenSextantExtractorResource;
import org.opensextant.tagger.solr.GeoSolrTagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaggerPool {

	private Map<String, BlockingQueue<Tagger>> poolMap = new HashMap<String, BlockingQueue<Tagger>>();
	private long docsProcessedCount;
	private long docsFailedCount;
	private Tika tika = new Tika();

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(TaggerPool.class);

	public TaggerPool(Properties prop) {

		String[] apps = prop.getProperty("os.service.appnames").split(",");

		for (String app : apps) {
			int poolSize = Integer.parseInt(prop.getProperty("os.service.app." + app + ".poolsize"));
			String impl = prop.getProperty("os.service.app." + app + ".impl");
			addTagger(app, impl, prop, poolSize);
		}

	}

	public Tagger getTagger(String taggerType) {

		Tagger tagger = null;
		try {
			tagger = poolMap.get(taggerType).take();
		} catch (InterruptedException e) {
			LOGGER.error("Couldn't get a processor from the pool", e);
		}

		if (tagger == null) {
			LOGGER.error("Couldn't get a processor from the pool");
		}

		return tagger;
	}

	public void returnTagger(Tagger tagger) {
		poolMap.get(tagger.getTaggerType()).add(tagger);
	}

	private void addTagger(String taggerType, String implType, Properties prop, int poolSize) {

		if (poolSize > 0) {
			ArrayBlockingQueue<Tagger> pool = new ArrayBlockingQueue<Tagger>(poolSize);

			for (int i = 0; i < poolSize; i++) {

				if (implType.equalsIgnoreCase("gate")) {
					GATETagger tagger = new GATETagger(taggerType, prop);
					pool.add(tagger);
				}

				if (implType.equalsIgnoreCase("geosolr")) {
					String solrHome = prop.getProperty("os.service.solr.home");
					GeoSolrTagger tagger = new GeoSolrTagger(solrHome);
					pool.add(tagger);
				}

				if (implType.equalsIgnoreCase("regex")) {
					String patterns = prop.getProperty("os.service.app." +  taggerType + ".patterns" );
					File patternFile = new File(patterns);
					RegexTagger tagger = new RegexTagger(patternFile);
					pool.add(tagger);
				}
				
			}

			poolMap.put(taggerType, pool);
		}
	}

	public Document tag(String taggerType, String content) {

		Document doc = new Document();
		doc.setContent(content);

		Tagger tagger = null;

		try {
			tagger = poolMap.get(taggerType).take();
		} catch (InterruptedException e) {
			LOGGER.error("Couldn't get a processor from the pool", e);
		}

		if (tagger == null) {
			LOGGER.error("Couldn't get a processor from the pool");
			return doc;
		}

		try {
			doc = tagger.tag(content);
		} catch (Exception e) {
			docsFailedCount++;
			LOGGER.error("Document failed to process", e);
		} finally {
			docsProcessedCount++;
			poolMap.get(taggerType).add(tagger);
		}

		return doc;
	}

	public Document tag(String taggerType, File file) {
		String content;
		try {
			content = tika.parseToString(file);
			return tag(taggerType, content);
		} catch (IOException | TikaException e) {
			LOGGER.error("Problem when translating document from file " + file.getName(), e);
		}
		return new Document();
	}

	public Document tag(String taggerType, URL url) {
		String content;
		try {
			content = tika.parseToString(url);
			return tag(taggerType, content);
		} catch (IOException | TikaException e) {
			LOGGER.error("Problem when translating document from URL" + url, e);
		}
		return new Document();
	}

	public void cleanup() {
		for (String name : poolMap.keySet()) {
			for (Tagger dp : poolMap.get(name)) {
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

	public int available(String taggerType) {
		return poolMap.get(taggerType).size();
	}

	public Map<String, Integer> available() {
		Map<String, Integer> avail = new HashMap<String, Integer>();
		for (String taggerType : poolMap.keySet()) {
			avail.put(taggerType, available(taggerType));
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
