package org.opensextant.tagger;

import java.util.List;
import java.util.Map;

import org.opensextant.tagger.geo.Place;

public interface Lexicon {

	public List<Map<String, Object>> query(String query);

	public List<Map<String, Object>> queryByName(String name, boolean fuzzy);

	public List<Place> geoQuery(String query);

	public List<Place> geoQueryByName(String placeName, String countryCode, boolean fuzzy);

}
