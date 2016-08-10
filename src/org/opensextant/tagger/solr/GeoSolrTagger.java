package org.opensextant.tagger.solr;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.common.params.CommonParams;
import org.opensextant.tagger.Document;
import org.opensextant.tagger.Lexicon;
import org.opensextant.tagger.Match;
import org.opensextant.tagger.geo.Place;
import org.opensextant.tagger.geo.PlaceCandidate;
import org.opensextant.tagger.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoSolrTagger extends SolrTagger {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeoSolrTagger.class);

	private static String coreName = "gazetteer";
	private static String matchFieldName = "name4matching";
	private static String searchFieldName = "name";
	/**
	 * Mapping from gazetteer codes to hierachical expression used on the Place
	 * object.
	 */
	private static Map<String, String> featureCodeMap = new HashMap<String, String>();

	static {
		featureCodeMap.put("A", "Geo.featureType.AdminRegion");
		featureCodeMap.put("P", "Geo.featureType.PopulatedPlace");
		featureCodeMap.put("V", "Geo.featureType.Vegetation");
		featureCodeMap.put("L", "Geo.featureType.Area");
		featureCodeMap.put("U", "Geo.featureType.Undersea");
		featureCodeMap.put("R", "Geo.featureType.Street");
		featureCodeMap.put("T", "Geo.featureType.Hypso");
		featureCodeMap.put("H", "Geo.featureType.Hydro");
		featureCodeMap.put("S", "Geo.featureType.SpotFeature");
	}

	private static final String APRIORI_NAME_RULE = "AprioriNameBias";

	private boolean tagAbbreviations = false;

	public GeoSolrTagger(String solrHome) {
		super(solrHome, coreName, matchFieldName, searchFieldName);
		this.matchParams.set(CommonParams.FQ, "search_only:false");
	}

	public GeoSolrTagger(String taggerType, Properties props) {
		super(props.getProperty("os.service.solr.home"), coreName, matchFieldName, searchFieldName);
	}

	public Document geoTag(String content) {

		List<Match> pcs = new ArrayList<Match>();

		Document doc = super.tag(content);

		for (Match match : doc.getMatchList()) {
			PlaceCandidate pc = convertToPlaceCandidate(match);
			if (pc != null) {
				pcs.add(pc);
			}
		}

		doc.setMatchList(pcs);
		return doc;
	}

	public Document geoTag(File file) {

		List<Match> pcs = new ArrayList<Match>();

		Document doc = super.tag(file);

		for (Match match : doc.getMatchList()) {
			PlaceCandidate pc = convertToPlaceCandidate(match);
			if (pc != null) {
				pcs.add(pc);
			}
		}

		doc.setMatchList(pcs);
		return doc;
	}

	public Document geoTag(URL url) {

		List<Match> pcs = new ArrayList<Match>();

		Document doc = super.tag(url);

		for (Match match : doc.getMatchList()) {
			PlaceCandidate pc = convertToPlaceCandidate(match);
			if (pc != null) {
				pcs.add(pc);
			}
		}

		doc.setMatchList(pcs);
		return doc;
	}

	public List<PlaceCandidate> geoMatch(String content) {

		List<PlaceCandidate> pcs = new ArrayList<PlaceCandidate>();

		List<Match> matches = super.match(content);

		for (Match match : matches) {
			PlaceCandidate pc = convertToPlaceCandidate(match);
			if (pc != null) {
				pcs.add(pc);
			}
		}

		return pcs;
	}

	public List<PlaceCandidate> geoMatch(File file) {

		List<PlaceCandidate> pcs = new ArrayList<PlaceCandidate>();

		List<Match> matches = super.match(file);

		for (Match match : matches) {
			PlaceCandidate pc = convertToPlaceCandidate(match);
			if (pc != null) {
				pcs.add(pc);
			}
		}

		return pcs;
	}

	public List<PlaceCandidate> geoMatch(URL url) {

		List<PlaceCandidate> pcs = new ArrayList<PlaceCandidate>();

		List<Match> matches = super.match(url);

		for (Match match : matches) {
			PlaceCandidate pc = convertToPlaceCandidate(match);
			if (pc != null) {
				pcs.add(pc);
			}
		}

		return pcs;
	}

	@Override
	public String getTaggerType() {
		return coreName;
	}

	@Override
	public boolean hasLexicon() {
		return true;
	}

	@Override
	public Lexicon getLexicon() {
		Lexicon lex = new SolrLexicon(solrClient, GeoSolrTagger.searchFieldName);
		return lex;
	}

	public boolean isTagAbbreviations() {
		return tagAbbreviations;
	}

	public void setTagAbbreviations(boolean tagAbbreviations) {
		this.tagAbbreviations = tagAbbreviations;
	}

	private PlaceCandidate convertToPlaceCandidate(Match match) {

		PlaceCandidate pc = new PlaceCandidate();

		String matchText = match.getMatchText();
		pc.setMatchText(matchText);
		pc.setStart(match.getStart());
		pc.setEnd(match.getEnd());
		pc.setType(match.getType());

		double nameBias = 0.0;
		List<Map<String, Object>> docs = (List<Map<String, Object>>) match.getFeatures().get(DOCS_FEATURENAME);
		for (Map<String, Object> doc : docs) {

			Place place = convertToPlace(doc);

			// don't tag abbreviations
			if (!this.tagAbbreviations && place.isAbbreviation()) {
				LOGGER.debug("Not tagging abbreviation:" + place + " for " + matchText);
				break;
			}
			pc.addPlaceWithScore(place, place.getIdBias());
			double nBias = place.getNameBias();
			if (nBias > nameBias) {
				nameBias = nBias;
			}

		}

		// if the max name bias seen >0; add apriori evidence
		if (nameBias > 0.0) {
			pc.addRuleAndConfidence(APRIORI_NAME_RULE, nameBias);
		}

		if (!pc.getPlaces().isEmpty()) {
			return pc;
		} else {
			return null;
		}

	}

	protected static Place convertToPlace(Map<String, Object> placeFeatures) {
		// create the basic Place
		Place place = new Place((String) placeFeatures.get("place_id"), (String) placeFeatures.get("name"));

		// add the expanded name
		place.setExpandedPlaceName((String) placeFeatures.get("name_expanded"));

		// set name type and nameTypeSystem
		place.setNameType(Utils.internString((String) placeFeatures.get("name_type")));
		place.setNameTypeSystem(Utils.internString((String) placeFeatures.get("name_type_system")));

		// set country coude using the cc (ISO2) value
		place.setCountryCode(Utils.internString((String) placeFeatures.get("cc")));

		// Set the admin values
		place.setAdmin1(Utils.internString((String) placeFeatures.get("adm1")));
		place.setAdmin2(Utils.internString((String) placeFeatures.get("adm2")));

		// map and set the feature class and code
		place.setFeatureClass(Utils.internString(featureCodeMap.get(placeFeatures.get("feat_class"))));
		place.setFeatureCode(Utils.internString((String) placeFeatures.get("feat_code")));

		// set the source
		place.setSource(Utils.internString((String) placeFeatures.get("source")));

		// set the geo

		String xy = (String) placeFeatures.get("geo");

		if (xy == null) {
			LOGGER.error("Blank geo field in record:" + placeFeatures);
			place.setLatitude(0.0);
			place.setLongitude(0.0);
		} else {
			String[] latLon = xy.split(",", 2);
			place.setLatitude(Double.parseDouble(latLon[0]));
			place.setLongitude(Double.parseDouble(latLon[1]));
		}

		// set the bias values
		place.setNameBias((Float) placeFeatures.get("name_bias"));
		place.setIdBias((Float) placeFeatures.get("id_bias"));

		return place;
	}

}
