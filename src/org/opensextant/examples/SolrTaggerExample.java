package org.opensextant.examples;

import java.util.List;
import java.util.Map;

import org.opensextant.placedata.Place;
import org.opensextant.placedata.PlaceCandidate;
import org.opensextant.tagger.Match;
import org.opensextant.tagger.solr.GeoSolrTagger;
import org.opensextant.tagger.solr.SolrTagger;

public class SolrTaggerExample {

	public static void main(String[] args) {

		String solrHome = args[0];
		String coreName = args[1];
		String matchField = args[2];

		String test = "We drove to the London";

		// System.setProperty("solr.home", args[0]);

		SolrTagger matcher = new SolrTagger(solrHome, coreName, matchField);
		GeoSolrTagger geoMatcher = new GeoSolrTagger(solrHome);

		List<? extends Match> matches = matcher.match(test);
		List<PlaceCandidate> geoMatches = geoMatcher.geoMatch(test);

		for (Match match : matches) {
			System.out.println(match.getMatchText());
			List<Map<String, Object>> payloads = match.getPayloads();
			System.out.println("Found " + matches.size() + " matches with " + payloads.size() + " payloads");

			for (Object pay : payloads) {
				System.out.println("\t" + pay);
			}
		}

		for (PlaceCandidate match : geoMatches) {
			System.out.println(match.getMatchText());
			List<Place> places = match.getPlaces();
			System.out.println("Found " + matches.size() + " matches with " + places.size() + " places");

			for (Place place : places) {
				System.out.println("\t" + place);
			}
		}

		matcher.cleanup();
		geoMatcher.cleanup();
	}

}
