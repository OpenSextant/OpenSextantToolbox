package org.opensextant.tagger;

import java.util.List;
import java.util.Map;

public interface Gazetteer {

	public List<Map<String, Object>> query(String query);

	public List<Map<String, Object>> queryByName(String name, boolean fuzzy);

}
