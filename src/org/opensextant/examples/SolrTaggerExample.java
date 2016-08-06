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
		String searchField = args[3];

		String testSentence = "We drove to the London";
		String testName = "al Kuwait";

		SolrTagger tagger = new SolrTagger(solrHome, coreName, matchField, searchField);
		GeoSolrTagger geoTagger = new GeoSolrTagger(solrHome);

		List<? extends Match> matches = tagger.match(testSentence);
		List<PlaceCandidate> geoMatches = geoTagger.geoMatch(testSentence);

		for (Match match : matches) {
			System.out.println(match.getMatchText());
			Map<String, Object> features = match.getFeatures();
			System.out.println("Found " + matches.size() + " with " + features.size() + " features");

			for (String pay : features.keySet()) {
				System.out.println("\t" + pay + "\t" +  features.get(pay));
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

		// do a simple name search using both fuzzy and not fuzzy matching
		System.out.println("Doing name search");
		List<Place> places = geoTagger.geoQueryByName(testName, false);
		System.out
				.println("Found " + places.size() + " places using placename= \"" + testName + "\"" + " fuzzy= false ");
		for (Place p : places) {
			System.out.println("\t" + p);
		}

		List<Place> fuzzyPlaces = geoTagger.geoQueryByName(testName, true);
		System.out.println(
				"Found " + fuzzyPlaces.size() + " places using placename= \"" + testName + "\"" + " fuzzy= true ");
		for (Place p : fuzzyPlaces) {
			System.out.println("\t" + p);
		}

		tagger.cleanup();
		geoTagger.cleanup();
	}

}
