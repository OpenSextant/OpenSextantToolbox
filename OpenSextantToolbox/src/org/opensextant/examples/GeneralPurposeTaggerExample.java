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

import gate.Annotation;
import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.util.persistence.PersistenceManager;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;


/** A simple example of how to use the OpenSextant General Purpose extractor pipeline.<br>
 Takes a directory as input and processes each file in the directory through the general purpose pipeline,
  printing some basic results of what it found.
 */
public class GeneralPurposeTaggerExample {

  /**
   * Instantiates a new general purpose tagger example.
   */
  private GeneralPurposeTaggerExample() {

  }
//TODO: Auto-generated Javadoc
  /**
   * The main method.
   *
   * @param args the arguments
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {
    // start time
    Long start = System.nanoTime();

    // file containing the pre-defined GATE application (GAPP)
    // this should point to opensextant-toolbox-2.0/LanguageResources/GAPPs/OpenSextant_GeneralPurpose.gapp
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
      // Document doc = Factory.newDocument(f.toURI().toURL(),"UTF-8");

      // put the document in the corpus
      corpus.add(doc);

      // run the application
      application.execute();

      // remove the document from the corpus
      corpus.clear();

      // document now has been tagged
      // now do something with the annotations found

      // get any annotation which has the "isEntity" feature
      // "isEntity" is the marker OpenSextant uses to distinquish finished entities
      // from candidates, intermediate results, building blocks and other internal stuff
      Set<String> featureNameSet = new HashSet<String>();
      featureNameSet.add("isEntity");
      gate.AnnotationSet entitySet = doc.getAnnotations().get(null, featureNameSet);

      // see what entity types we found in this document
      Set<String> entityTypesFound = entitySet.getAllTypes();
      
      // loop over all found entities and print some basic info
      System.out.println("Document " + doc.getName() + " contains annotations of type (count):");
      for (String a : entityTypesFound) {
        // get all annotations of a type 
        gate.AnnotationSet tmpSet = entitySet.get(a);
        System.out.println("\t" + a + " (" + tmpSet.size() + ")");
        // loop over all instance of this type and print some basic info
        for (Annotation s : tmpSet) {
          // get a clean string representation of the tagged text
          String text = gate.Utils.cleanStringFor(doc, s);
          // get the taxonomic categorization for this entity
          String taxo = (String) s.getFeatures().get("hierarchy");
          // could also get the start/end points, other features ... 
          System.out.println("\t\t" + text + " (" + taxo + ")");
        }

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
