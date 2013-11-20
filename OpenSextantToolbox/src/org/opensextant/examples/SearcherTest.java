package org.opensextant.examples;

import java.util.List;

import org.opensextant.matching.MatcherFactory;
import org.opensextant.matching.PlacenameSearcher;
import org.opensextant.placedata.Place;
import org.opensextant.placedata.ScoredPlace;


/**
 * Simple example of using the PlacenameSearcher to look up names in the gazetteer.
 */
public class SearcherTest {

  private SearcherTest() {

  }

  public static void main(String[] args) {

    String solrHome = "";
    if (args.length > 0) {
      solrHome = args[0];
    }

    System.out.println("Configuring");
    MatcherFactory.config(solrHome);
    System.out.println("Starting");
    MatcherFactory.start();

    PlacenameSearcher s = MatcherFactory.getSearcher();

    if (null == s) {
      System.err.println("Got a null Searcher from Factory.");
      return;
    }

    List<Place> placesFound;

    // parameters used in the sample searches
    String genericQuery = "source:ADHOC";
    String nameQuery = "Bogata";
    Double lat = 36.95024;
    Double lon = 72.92037;
    Double distance = 10.0; // kilometers

    // do a simple name search using both fuzzy and not fuzzy matching
    System.out.println("Doing name search");
    placesFound = s.searchByPlaceName(nameQuery, true);
    System.out.println("Found " + placesFound.size() + " places using placename= \"" + nameQuery + "\""
        + " fuzzy= true ");
    for (Place p : placesFound) {
      System.out.println("\t" + p);
    }

    placesFound = s.searchByPlaceName(nameQuery, false);
    System.out.println("Found " + placesFound.size() + " places using placename= \"" + nameQuery + "\""
        + " fuzzy= false ");
    for (Place p : placesFound) {
      System.out.println("\t" + p);
    }

    // do a geo (circle) search around a given point
    System.out.println("Doing geo search");
    List<ScoredPlace> placesWithDist = s.searchByCircle(lat, lon, distance);
    System.out.println("Found " + placesWithDist.size() + " places within " + distance + " kilometers of (" + lat + ","
        + lon + ")");
    for (ScoredPlace p : placesWithDist) {
      System.out.println("\t" + p.getPlace().getPlaceName() + " (" + p.getPlace().getLatitude() + "," + p.getPlace().getLongitude() + ") is "
          + p.getScore() + " kms from center");
    }

    // do a search by passing in an arbitrary solr query
    System.out.println("Doing generic query");
    placesFound = s.searchByQueryString(genericQuery);
    System.out.println("Found " + placesFound.size() + " places using query= \"" + genericQuery + "\"");
    for (Place p : placesFound) {
      System.out.println("\t" + p);
    }

    // make sure all gets written
    System.out.flush();
    s.cleanup();

  }

}
