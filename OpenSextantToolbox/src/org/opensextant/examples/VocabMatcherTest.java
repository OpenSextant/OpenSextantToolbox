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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.opensextant.matching.MatcherFactory;
import org.opensextant.matching.VocabMatcher;
import org.opensextant.vocab.Vocab;
import org.opensextant.vocab.VocabMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * Simple example of using the VocabMatcher which uses the Solr gazetteer to find vocabulary in text.
 */
public class VocabMatcherTest {

  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(VocabMatcherTest.class);

  /**
   * Instantiates a new matcher test.
   */
  private VocabMatcherTest() {

  }

  /**
   * The main method.
   * @param args
   *          the arguments
   */
  public static void main(String[] args) {

    // the file with some text to be processed
    File testText = new File(args[0]);

    // location of solr containg the gazetteer
    // could be a directory, URL or missing
    String solrHome = "";
    if (args.length == 2) {
      LOGGER.info("Using supplied arg for location of solr gazetteer");
      solrHome = args[1];
    } else {
      LOGGER.info("No arg supplied for location of solr gazetteer. Using environment variable");
    }

    // configure and start the Matcher Factory
    MatcherFactory.config(solrHome);
    MatcherFactory.start();

    // get a matcher
    VocabMatcher m = MatcherFactory.getVocabMatcher();
    // check to see if its there
    if (null == m) {
      LOGGER.error("Got a null Matcher from Factory.");
      return;
    }

    // get some sample text
    String sampleText;
    try {
      sampleText = FileUtils.readFileToString(testText, "UTF-8");
    } catch (IOException e) {
      LOGGER.error("Exception reading text from file" + testText.getName(), e);
      return;
    }

    // send the sample text to be tagged
    List<VocabMatch> matches = m.matchText(sampleText, "test document");

    // see what got tagged
    LOGGER.info("Found " + matches.size() + " matches");
    for (VocabMatch mt : matches) {
      String matchText = mt.getMatchText();
      List<Vocab> vs = mt.getVocabs();
      LOGGER.info("\t" + matchText + " " + vs.size() + " possibilities:" + vs);
    }

    // cleanup the matcher
    m.cleanup();

  }

}
