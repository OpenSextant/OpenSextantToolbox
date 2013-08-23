package org.opensextant.examples;

import java.util.List;

import org.opensextant.matching.MatcherFactory;
import org.opensextant.matching.PlacenameMatcher;
import org.opensextant.placedata.Place;
import org.opensextant.placedata.PlaceCandidate;

public class MatcherTest {

  private MatcherTest() {

  }

  /**
   * @param args
   */
  public static void main(String[] args) {

    // location of the solr containg the gazetteer
    // could be a directory, URL or missing
    String solrHome = "";
    if (args.length == 0) {
      solrHome = args[0];
    }

    // configure and start the Matcher Factory
    MatcherFactory.config(solrHome);
    MatcherFactory.start();

    // get a matcher
    PlacenameMatcher m = MatcherFactory.getMatcher();
    // check to see if its there
    if (null == m) {
      System.err.println("Got a null Matcher from Factory.");
      return;
    }

    String sampleText = "We drove to London,Kabul,Paris and Hoboken";

    // choose to NOT tag abbreviations
    m.tagAbbreviations(false);
    // send the sample text to be tagged
    List<PlaceCandidate> cands = m.matchText(sampleText, "test document");

    // see what got tagged
    System.out.println("Without abbreviations,found " + cands.size() + " place candidates");
    for (PlaceCandidate pc : cands) {
      String placeName = pc.getPlaceName();
      int numCands = pc.getPlaces().size();
      List<Place> places = pc.getPlaces();
      Place samplePlace = pc.getBestPlace();
      System.out.println("\t" + placeName + " could be " + numCands + " places, like " + samplePlace);
    }

    // turn on tagging abbreviations
    m.tagAbbreviations(true);
    List<PlaceCandidate> cands2 = m.matchText(sampleText, "test document");

    // see what got tagged this time
    System.out.println("Tagging abbreviations,found " + cands2.size() + " place candidates");
    for (PlaceCandidate pc : cands2) {
      String placeName = pc.getPlaceName();
      int numCands = pc.getPlaces().size();
      Place samplePlace = pc.getBestPlace();

      System.out.println("\t" + placeName + " could be " + numCands + " places, like " + samplePlace);
    }
    m.cleanup();

  }

}
