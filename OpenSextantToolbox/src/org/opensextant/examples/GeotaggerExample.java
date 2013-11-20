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

/** A simple example of how to use the OpenSextant Geotagger pipeline.<br>
Takes a directory as input and processes each file in the directory through the geotagger,
 printing some basic results of what it found.
*/
public class GeotaggerExample {

  private GeotaggerExample() {

  }

  public static void main(String[] args) throws Exception {
    // start time
    Long start = System.nanoTime();

    // file containing the pre-defined GATE application (GAPP)
    // this should point to opensextant-toolbox-2.0/LanguageResources/GAPPs/OpenSextant_Geotagger.gapp
    File gappFile = new File(args[0]);

    // directory containing the files to be processed
    File inDir = new File(args[1]);

    System.out.println("Initializing");
    
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

    System.out.println("Done Initializing");
    Double initTime = (System.nanoTime() - start)/ 1000000000.0;

    
    // run the files through the tagger
    for (File f : files) {

      // create a GATE document from the file contents
      String contents = FileUtils.readFileToString(f, "UTF-8");
      Document doc = Factory.newDocument(contents);
      doc.setName(f.getName());

      // or create a GATE document directly from the file
      // Document doc = Factory.newDocument(f.toURI().toURL());

      // put the document in the corpus
      corpus.add(doc);

      // run the application
      application.execute();

      // remove the document from the corpus
      corpus.clear();

      // document now has been tagged
      // now do something with the annotations found

      // get the geotagging results: annotations of type GEOCOORD or PLACE
      AnnotationSet geocoordResults = doc.getAnnotations().get("GEOCOORD");
      AnnotationSet placeNameResults = doc.getAnnotations().get("PLACE");

      System.out.println("In document " + doc.getName());
      System.out.println("\tGeocoords Found (" + geocoordResults.size() + ")");
      for (Annotation a : geocoordResults) {
        // get a clean string for the geocoord found in the text
        String text = gate.Utils.cleanStringFor(doc, a);
        // get the features of this annotation
        FeatureMap fm = a.getFeatures();
        // get the Geocoord object attached to this annotation
        Geocoord coord = (Geocoord) fm.get("geo");
        double lat = coord.getLatitude();
        double lon = coord.getLongitude();
        System.out.println("\t\t" + text + "->" + lat + "," + lon);
      }

      System.out.println("\tPlaces Found (" + placeNameResults.size() + ")");
      for (Annotation a : placeNameResults) {
        // get a clean string for the place found in the text
        String text = gate.Utils.cleanStringFor(doc, a);
        // get the features of this annotation
        FeatureMap fm = a.getFeatures();
        // get the highest ranked place attached to this annotation
        Place pl = (Place) fm.get("bestPlace");
        String name = pl.getPlaceName();
        String cc = pl.getCountryCode();
        Geocoord coord = pl.getGeocoord();
        double lat = coord.getLatitude();
        double lon = coord.getLongitude();

        // could also see lots more details: alternative places, scores, evidence ...
        // by getting the placeCandidate object attached
        // PlaceCandidate pcObj = (PlaceCandidate) fm.get("placeCandidate");

        System.out.println("\t\t" + text + "->" + name + "," + cc + "(" + lat + "," + lon + ")");
      }

      // make sure all gets written
      System.out.flush();
      
      // cleanup the document, the file and the content
      Factory.deleteResource(doc);
      contents = null;
      f = null;

    } // end file loop

    // cleanup the corpus and application
    Factory.deleteResource(corpus);
    application.cleanup();

    // finish time
    Long end = System.nanoTime();

    // print some summary stats
    double totalDuration = (end - start) / 1000000000.0;
    double rate = numDocs / (totalDuration - initTime);

    System.out.println("Document count=" + numDocs + "\t" + "Total time=" + totalDuration + "\t" + "Rate=" + rate
        + " documents/sec");

  }

}
