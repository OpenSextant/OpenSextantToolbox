package org.opensextant.examples;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.util.persistence.PersistenceManager;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.opensextant.placedata.Geocoord;
import org.opensextant.placedata.Place;

/* A "canonical" GATE application
 Takes a GATE application (GAPP) file and directory as input
 processes each file in the directory through the process defined in the GAPP.
 */
public class GeotaggerExample {

  private GeotaggerExample() {

  }

  public static void main(String[] args) throws Exception {
    // start time
    Long start = System.nanoTime();

    // file containing the pre-defined GATE application (GAPP)
    File gappFile = new File(args[0]);

    // directory containing the files to be processed
    File inDir = new File(args[1]);

    // get the list of files to be processed
    Collection<File> files = FileUtils.listFiles(inDir, null, true);
    int numDocs = files.size();

    // initialize GATE
    Gate.init();

    // load the saved application from the GAPP file
    CorpusController application = (CorpusController) PersistenceManager.loadObjectFromFile(gappFile);

    // the corpus (to be re-used)
    Corpus corpus = Factory.newCorpus("Test Corpus");

    // associate the corpus with the application
    application.setCorpus(corpus);

    // process the files, one at a time
    for (File f : files) {

      // create a GATE document from the file contents
      String contents = FileUtils.readFileToString(f, "UTF-8");
      Document doc = Factory.newDocument(contents);

      // or create a GATE document directly from the file
      // Document doc = Factory.newDocument(f.toURI().toURL());

      // put the document in the corpus
      corpus.add(doc);

      // run the application
      application.execute();

      // remove the document from the corpus
      corpus.clear();

      // this is where we would do something with
      // the annotations found

      // get the geotagging results: annotations of type GEOCOORD or PLACE
      AnnotationSet geocoordResults = doc.getAnnotations().get("GEOCOORD");
      AnnotationSet placeNameResults = doc.getAnnotations().get("PLACE");

      System.out.println("Geocoords Found (" + geocoordResults.size() + ")");
      for (Annotation a : geocoordResults) {
        String text = gate.Utils.cleanStringFor(doc, a);
        FeatureMap fm = a.getFeatures();
        Geocoord coord = (Geocoord) fm.get("geo");
        double lat = coord.getLatitude();
        double lon = coord.getLongitude();
        System.out.println("\t" + text + "->" + lat + "," + lon);
      }

      System.out.println("Places Found (" + placeNameResults.size() + ")");
      for (Annotation a : placeNameResults) {
        String text = gate.Utils.cleanStringFor(doc, a);
        FeatureMap fm = a.getFeatures();
        Place pl = (Place) fm.get("bestPlace");
        String name = pl.getPlaceName();
        String cc = pl.getCountryCode();
        Geocoord coord = pl.getGeocoord();
        double lat = coord.getLatitude();
        double lon = coord.getLongitude();
        System.out.println("\t" + text + "->" + name + "," + cc + "(" + lat + "," + lon + ")");
      }

      // cleanup
      Factory.deleteResource(doc);
      contents = null;
      f = null;

    } // end file loop

    // cleanup
    Factory.deleteResource(corpus);
    application.cleanup();

    // finish time
    Long end = System.nanoTime();

    // print some summary stats
    double totalDuration = (end - start) / 1000000000.0;
    double rate = numDocs / totalDuration;

    System.out.println("Document count=" + numDocs + "\t" + "Total time=" + totalDuration + "\t" + "Rate=" + rate
        + " documents/sec");

  }

}
