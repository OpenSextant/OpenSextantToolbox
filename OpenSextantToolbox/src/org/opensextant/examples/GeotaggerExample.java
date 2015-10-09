/*
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

// TODO: Auto-generated Javadoc
/**
 * A simple example of how to use the OpenSextant Geotagger pipeline.<br>
 * Takes a directory as input and processes each file in the directory through the geotagger, printing some basic
 * results of what it found.
 */
public class GeotaggerExample {

  /**
   * Instantiates a new geotagger example.
   */
  private GeotaggerExample() {

  }

  /**
   * The main method.
   * @param args
   *          the arguments
   * @throws Exception
   *           the exception
   */
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
    Double initTime = (System.nanoTime() - start) / 1000000000.0;

    // run the files through the tagger
    for (File f : files) {

      // If your documents are text, HTML,XML,Word, PDF, ....
      // create a GATE document directly from the file
      Document doc = Factory.newDocument(f.toURI().toURL(), "UTF-8");

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
        fm.clear();
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
        fm.clear();
        // could also see lots more details: alternative places, scores, evidence ...
        // by getting the placeCandidate object attached
        System.out.println("\t\t" + text + "->" + name + "," + cc + "(" + lat + "," + lon + ")");
      }

      // make sure all gets written
      System.out.flush();

      // cleanup the document, the file and the content
      Factory.deleteResource(doc);
      doc = null;
      f = null;
      geocoordResults = null;
      placeNameResults = null;

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
