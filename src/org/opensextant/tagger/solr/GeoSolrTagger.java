package org.opensextant.tagger.solr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.params.CommonParams;
import org.opensextant.placedata.Place;
import org.opensextant.placedata.PlaceCandidate;
import org.opensextant.tagger.Match;
import org.opensextant.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoSolrTagger extends SolrTagger {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeoSolrTagger.class);

	private static String coreName = "gazetteer";
	private static String fieldName = "name4matching";
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
		super(solrHome, coreName, fieldName);
		this.matchParams.set(CommonParams.FQ, "search_only:false");
	}

	public List<PlaceCandidate> geoMatch(String buffer) {

		List<PlaceCandidate> pcs = new ArrayList<PlaceCandidate>();

		List<Match> matches = super.match(buffer);

		for (Match match : matches) {
			PlaceCandidate pc = convertToPlaceCandidate(match);
			if (pc != null) {
				pcs.add(pc);
			}
		}
		return pcs;
	}

	private PlaceCandidate convertToPlaceCandidate(Match match) {

		PlaceCandidate pc = new PlaceCandidate();

		String matchText = match.getMatchText();
		pc.setMatchText(matchText);
		pc.setStart(match.getStart());
		pc.setEnd(match.getEnd());
		pc.setType(match.getType());

		double nameBias = 0.0;
		for (Map<String, Object> payloads : match.getPayloads()) {

			Place place = convertToPlace(payloads);

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

	public boolean isTagAbbreviations() {
		return tagAbbreviations;
	}

	public void setTagAbbreviations(boolean tagAbbreviations) {
		this.tagAbbreviations = tagAbbreviations;
	}

	protected Place convertToPlace(Map<String, Object> placeFeatures) {
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
