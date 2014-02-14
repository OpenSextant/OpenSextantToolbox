/**
 Copyright 2009-2013 The MITRE Corporation.
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 * **************************************************************************
 *                          NOTICE
 * This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 **/
package org.opensextant.examples;

import java.util.List;

import org.opensextant.matching.MatcherFactory;
import org.opensextant.matching.PlacenameSearcher;
import org.opensextant.placedata.Place;
import org.opensextant.placedata.ScoredPlace;

// TODO: Auto-generated Javadoc
/**
 * Simple example of using the PlacenameSearcher to look up names in the gazetteer.
 */
public class SearcherTest {

  /**
   * Instantiates a new searcher test.
   */
  private SearcherTest() {

  }

  /**
   * The main method.
   * @param args
   *          the arguments
   */
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
      System.out.println("\t" + p.getPlace().getPlaceName() + " (" + p.getPlace().getLatitude() + ","
          + p.getPlace().getLongitude() + ") is " + p.getScore() + " kms from center");
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
