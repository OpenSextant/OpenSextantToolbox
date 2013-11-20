package org.opensextant.examples;

import java.io.File;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.opensextant.matching.MatcherFactory;
import org.opensextant.matching.PlacenameMatcher;
import org.opensextant.placedata.Place;
import org.opensextant.placedata.PlaceCandidate;

/**
 * Simple example of using the PlaceNameMatcher which uses the Solr gazetteer to find candidate place names in text
 */
public class MatcherTest {

  private MatcherTest() {

  }

  public static void main(String[] args) {

    // the file with some text to be processed
    File testText = new File(args[0]);
    
    // location of solr containg the gazetteer
    // could be a directory, URL or missing
    String solrHome = "";
    if (args.length == 2) {
      System.out.println("Using supplied arg for location of solr gazetteer");
      solrHome = args[1];
    }else{
      System.out.println("No arg supplied for location of solr gazetteer. Using environment variable");
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

    // get some sample text
    String sampleText;
    try {
      sampleText = FileUtils.readFileToString(testText, "UTF-8");
    } catch (IOException e) {
      System.err.println("Exception reading text from file" + testText.getName());
      return;
    }

    // choose to NOT tag questionable abbreviations
    m.tagAbbreviations(false);

    // send the sample text to be tagged
    List<PlaceCandidate> cands = m.matchText(sampleText, "test document");

    // see what got tagged
    System.out.println("Without questionable abbreviations,found " + cands.size() + " place candidates");
    for (PlaceCandidate pc : cands) {
      String placeName = pc.getPlaceName();

      // if you want all possible places
      List<Place> allPlaces = pc.getPlaces();

      // if you want only the apriori most likely
      Place samplePlace = pc.getBestPlace();

      System.out.println("\t" + placeName + " could be " + allPlaces.size() + " places, like " + samplePlace);
    }

    // Now turn on tagging of questionable abbreviations and tag again to see differences
    m.tagAbbreviations(true);
    List<PlaceCandidate> cands2 = m.matchText(sampleText, "test document");

    // see what got tagged this time
    System.out.println("Tagging questionable abbreviations,found " + cands2.size() + " place candidates");
    for (PlaceCandidate pc : cands2) {
      String placeName = pc.getPlaceName();

      // if you want all possible places
      List<Place> allPlaces = pc.getPlaces();

      // if you want only the apriori most likely
      Place samplePlace = pc.getBestPlace();

      System.out.println("\t" + placeName + " could be " + allPlaces.size() + " places, like " + samplePlace);
    }

    // make sure all gets written
    System.out.flush();
    
    // cleanup the matcher
    m.cleanup();

  }

}
