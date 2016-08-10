package org.opensextant.tagger.solr;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.opensextant.tagger.Document;
import org.opensextant.tagger.Lexicon;
import org.opensextant.tagger.Match;
import org.opensextant.tagger.Tagger;
import org.opensextant.tagger.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrTagger implements Tagger {

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(SolrTagger.class);

	protected static EmbeddedSolrServer solrClient;

	private String solrHome = "";
	private String taggerType = "";
	private String matchFieldName = "";
	private String searchFieldName = "";

	private SolrTaggerRequest tagRequest;
	private Map<Integer, Map<String, Object>> idMap = new HashMap<Integer, Map<String, Object>>(100);

	public ModifiableSolrParams matchParams = new ModifiableSolrParams();

	/** The matching request handler. */
	private static final String MATCH_REQUESTHANDLER = "/tag";
	private static final String SEARCH_FIELD = "name";
	private static final String MATCH_FIELD = "name4matching";
	protected static final String DOCS_FEATURENAME = "matchingDocuments";

	private Tika tika = new Tika();
	
	public SolrTagger(String taggerType, Properties props) {

		String tmpSolrHome = props.getProperty("os.service.solr.home");
		String tmpMatchFieldName = props.getProperty("os.service.solr." + taggerType + ".matchField", MATCH_FIELD);
		String tmpSearchFieldName = props.getProperty("os.service.solr." + taggerType + ".searchField", SEARCH_FIELD);

		initialize(tmpSolrHome, taggerType, tmpMatchFieldName, tmpSearchFieldName);

	}

	public SolrTagger(String solrHome, String coreName, String matchField, String searchField) {

		initialize(solrHome, coreName, matchField, searchField);

	}

	private void initialize(String solrhome, String taggerType, String matchFieldName, String searchFieldName) {

		String tmpSolrHome = solrhome;

		if (tmpSolrHome == null || tmpSolrHome.isEmpty()) {
			LOGGER.info("No value given for Solr home. Trying solr.home system property");
			tmpSolrHome = System.getProperty("solr.home");

			if (tmpSolrHome == null || tmpSolrHome.isEmpty()) {
				LOGGER.error("Cannot initialize Solr Tagger: No value given for Solr home and no system property. ");
				return;
			}
		}

		this.solrHome = tmpSolrHome;
		LOGGER.info("Using Solr home = " + this.solrHome);
		this.matchFieldName = matchFieldName;
		this.searchFieldName = searchFieldName;
		this.taggerType = taggerType;

		// if not already there
		if (solrClient == null) {
			CoreContainer solrContainer = new CoreContainer(solrHome);
			solrContainer.load();

			if (!solrContainer.getAllCoreNames().contains(taggerType)) {
				LOGGER.error("No Solr core named " + taggerType + ". Choices are " + solrContainer.getAllCoreNames());
			}

			// note taggerType here is just the default core for requests, not
			// the only core
			solrClient = new EmbeddedSolrServer(solrContainer, this.taggerType);
		}

		matchParams.set(CommonParams.QT, MATCH_REQUESTHANDLER);
		matchParams.set(CommonParams.FL, "*");
		matchParams.set("tagsLimit", 100000);
		matchParams.set(CommonParams.ROWS, 100000);
		matchParams.set("subTags", false);
		matchParams.set("matchText", false);
		matchParams.set("overlaps", "LONGEST_DOMINANT_RIGHT");
		matchParams.set("field", this.matchFieldName);

	}

	public Document tag(String content) {

		Document doc = new Document();
		doc.setContent(content);
		doc.setMatchList(this.match(content));

		return doc;
	}

	@Override
	public Document tag(File file) {

		Document doc = new Document();
		
		try {
			String content = tika.parseToString(file);
			doc.setContent(content);
			doc.setMatchList(this.match(content));
		} catch (IOException | TikaException e) {
			LOGGER.error("Could not get text from file " + file.getName(),e);
		}

		return doc;
	}

	@Override
	public Document tag(URL url) {

		Document doc = new Document();
		
		try {
			String content = tika.parseToString(url);
			doc.setContent(content);
			doc.setMatchList(this.match(content));
		} catch (IOException | TikaException e) {
			LOGGER.error("Could not get text from URL " + url,e);
		}

		return doc;
	}

	@Override
	public List<Match> match(String content) {

		List<Match> matches = new ArrayList<Match>();
		// Setup request to tag
		tagRequest = new SolrTaggerRequest(matchParams, SolrRequest.METHOD.POST);
		tagRequest.setInput(content);

		QueryResponse response = null;

		try {
			response = tagRequest.process(solrClient);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Got exception when attempting to match ", e);
			return matches;
		}

		// Process Solr Response
		SolrDocumentList docList = response.getResults();

		// TODO convert this section to use a StreamingResponseCallback

		// convert each solrdoc (a match) to a feature and add to id map
		for (SolrDocument solrDoc : docList) {
			Integer id = (Integer) solrDoc.getFirstValue("id");
			Map<String, Object> fields = solrDoc.getFieldValueMap();
			idMap.put(id, fields);
		}

		@SuppressWarnings("unchecked")
		List<NamedList<?>> tags = (List<NamedList<?>>) response.getResponse().get("tags");

		for (NamedList<?> tag : tags) {
			Match match = new Match();
			int x1 = -1, x2 = -1;

			// get the start, end and list of matching place IDs
			x1 = (Integer) tag.get("startOffset");
			x2 = (Integer) tag.get("endOffset");

			@SuppressWarnings("unchecked")
			List<Integer> idList = (List<Integer>) tag.get("ids");

			// populate the Match
			match.setStart(x1);
			match.setEnd(x2);
			match.setMatchText(content.substring(x1, x2));
			match.setType(this.taggerType);
			List<Map<String, Object>> matchingDocs = new ArrayList<Map<String, Object>>();
			for (Integer id : idList) {
				Map<String, Object> fields = idMap.get(id);
				matchingDocs.add(fields);
			}
			match.addFeature(DOCS_FEATURENAME, matchingDocs);
			matches.add(match);
		}

		return matches;

	}

	@Override
	public List<Match> match(File file) {


		
		try {
			return this.match(tika.parseToString(file));
		} catch (IOException | TikaException e) {
			LOGGER.error("Could not get text from file " + file.getName(),e);
		}

		return new ArrayList<Match>();
	}

	@Override
	public List<Match> match(URL url) {


		try {
			return this.match(tika.parseToString(url));
		} catch (IOException | TikaException e) {
			LOGGER.error("Could not get text from file " + url,e);
		}
		return new ArrayList<Match>();
	}

	@Override
	public String getTaggerType() {
		return taggerType;
	}

	@Override
	public boolean hasLexicon() {
		return true;
	}

	@Override
	public Lexicon getLexicon() {
		Lexicon lex = new SolrLexicon(solrClient, this.searchFieldName);
		return lex;
	}

	public void cleanup() {
		try {
			SolrTagger.solrClient.close();
		} catch (IOException e) {
			LOGGER.error("Error closing Solr Matcher:", e);
		}
	}

	public void setCoreName(String name, String matchField) {
		matchParams.set(CommonParams.FL, Utils.getFieldNames(solrClient, name));
		matchParams.set("field", matchField);
	}

}
