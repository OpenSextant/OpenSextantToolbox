package org.opensextant.tagger.regex.geo;

import java.util.Comparator;

import org.opensextant.tagger.regex.RegexAnnotation;

public class GeocoordComparator implements Comparator<RegexAnnotation> {

	@Override
	public int compare(RegexAnnotation o1, RegexAnnotation o2) {

		if (!"GEOCOORD".equalsIgnoreCase(o1.getType()) || !"GEOCOORD".equalsIgnoreCase(o2.getType())) {
			return 0;
		}

		String fam1 = (String) o1.getFeatures().get("geoFamily");
		String fam2 = (String) o2.getFeatures().get("geoFamily");

		// if they havent been normalized, no family info
		if (fam1 == null || fam2 == null) {
			return 0;
		}

		if ("DM".equalsIgnoreCase(fam1) && "DMS".equalsIgnoreCase(fam2)) {
			return 1;
		}
		if ("DMS".equalsIgnoreCase(fam1) && "DM".equalsIgnoreCase(fam2)) {
			return -1;
		}

		long len1 = o1.getEnd() - o1.getStart();
		long len2 = o2.getEnd() - o2.getStart();

		return Long.compare(len1, len2);

	}

}
