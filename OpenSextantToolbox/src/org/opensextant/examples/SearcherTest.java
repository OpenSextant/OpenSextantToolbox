package org.opensextant.examples;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.opensextant.matching.MatcherFactory;
import org.opensextant.matching.PlacenameMatcher;
import org.opensextant.matching.PlacenameSearcher;
import org.opensextant.placedata.Place;
import org.opensextant.placedata.PlaceCandidate;

public class SearcherTest {

  private SearcherTest() {

  }

  /**
   * @param args
   */
  public static void main(String[] args) {

    String solrHome = args[0];

    MatcherFactory.config(solrHome);
    MatcherFactory.start();

    PlacenameSearcher s = MatcherFactory.getSearcher();

    if (null == s) {
      System.err.println("Got a null Searcher from Factory.");
      return;
    }

    List<Place> places = s.search("source:ADHOC");
    System.out.println("Found " + places.size() + " places");
    for (Place p : places) {
      System.out.println(p);
    }

    //System.exit(0);

    PlacenameMatcher m = MatcherFactory.getMatcher();
    if (null == m) {
      System.err.println("Got a null Matcher from Factory.");
      return;
    }


    Collection<File> files = FileUtils.listFiles(new File("../LanguageResources/TestData/testDocs"), null, true);
    m.tagAbbreviations(true);

    for (File f : files) {

      String contents = "";
      try {
        contents = FileUtils.readFileToString(f);
      } catch (IOException e) {
        System.err.println("Could not get contents from " + f.getName());
        break;
      }

      List<PlaceCandidate> cands = m.matchText(contents, "test text");
    }

    m.tagAbbreviations(true);
    List<PlaceCandidate> cands = m.matchText("We drove over London Bridge, which is in Springfield,IN", "test text");

    System.out.println("true =->" + cands.size());
    for (PlaceCandidate pc : cands) {
      System.out.println("\t" + pc.getPlaceName());
    }

    m.tagAbbreviations(false);
    List<PlaceCandidate> cands2 = m.matchText("We drove over London Bridge, which is in Springfield,IN", "test text");

    System.out.println("false =->" + cands2.size());
    for (PlaceCandidate pc : cands2) {
      System.out.println("\t" + pc.getPlaceName());
    }

    s.cleanup();
    m.cleanup();

  }

}
