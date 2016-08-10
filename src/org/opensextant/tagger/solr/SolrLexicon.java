package org.opensextant.tagger.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.opensextant.tagger.Lexicon;
import org.opensextant.tagger.geo.Place;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrLexicon implements Lexicon {

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(SolrLexicon.class);
	public ModifiableSolrParams searchParams = new ModifiableSolrParams();
	protected EmbeddedSolrServer solrClient;
	private String searchFieldName = "";
	private String countryFieldName ="cc";

	public SolrLexicon(EmbeddedSolrServer solrClient, String searchFieldName) {

		this.solrClient = solrClient;
		this.searchFieldName = searchFieldName;
		searchParams.set(CommonParams.Q, "*:*");
		searchParams.set(CommonParams.FL, "* score");
		searchParams.set(CommonParams.DF, searchFieldName);
		searchParams.set(CommonParams.ROWS, 100000);

	}

	@Override
	public List<Map<String, Object>> query(String query) {
		ModifiableSolrParams srchParams = new ModifiableSolrParams(searchParams);
		srchParams.set("q", query);
		return search(srchParams);
	}

	@Override
	public List<Map<String, Object>> queryByName(String name, boolean fuzzy) {
		ModifiableSolrParams srchParams = new ModifiableSolrParams(searchParams);
		String query = "name:";
		if (fuzzy) {
			query = query + name + "~0.80";
		} else {
			query = query + "\"" + name + "\"";
		}

		srchParams.set("defType", "edismax");

		srchParams.set("q", query);
		return search(srchParams);
	}

	@Override
	public List<Place> geoQuery(String query) {
		ModifiableSolrParams srchParams = new ModifiableSolrParams(searchParams);
		srchParams.set("q", query);
		return geoSearch(srchParams);
	}

	@Override
	public List<Place> geoQueryByName(String placeName, String countryCode, boolean fuzzy) {
		ModifiableSolrParams srchParams = new ModifiableSolrParams(searchParams);

		String query = "";

		if(countryCode != null && !countryCode.isEmpty()){
			query = countryFieldName + ":\"" + countryCode + "\" AND ";
		}
		
		if (!fuzzy) {
			query = query + searchFieldName + ":\"" + placeName + "\"";
		} else {

			String[] pieces = placeName.split("\\s");

			query = searchFieldName + ":" + pieces[0] + "~80";

			for (int i = 1; i < pieces.length; i++) {
				query = query + " AND " + searchFieldName + ":" + pieces[i] + "~80";
			}

		}

		// srchParams.set("defType", "edismax");

		srchParams.set("q", query);
		return geoSearch(srchParams);
	}

	private List<Map<String, Object>> search(ModifiableSolrParams prms) {

		List<Map<String, Object>> places = new ArrayList<Map<String, Object>>();

		QueryResponse response = null;
		try {
			response = solrClient.query(prms);
			if (response != null) {
				SolrDocumentList docList = response.getResults();
				for (SolrDocument d : docList) {
					places.add(d.getFieldValueMap());
				}
			}
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Got exception when processing query.", e);
			return places;
		}
		return places;
	}

	private List<Place> geoSearch(ModifiableSolrParams prms) {

		List<Place> places = new ArrayList<Place>();

		QueryResponse response = null;
		try {
			response = solrClient.query(prms);
			if (response != null) {
				SolrDocumentList docList = response.getResults();
				for (SolrDocument d : docList) {

					places.add(GeoSolrTagger.convertToPlace(d.getFieldValueMap()));
				}
			}
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Got exception when processing query.", e);
			return places;
		}
		return places;

	}
}
