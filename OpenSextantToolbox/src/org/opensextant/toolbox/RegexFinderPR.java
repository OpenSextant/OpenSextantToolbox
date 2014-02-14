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
package org.opensextant.toolbox;

import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.InvalidOffsetException;

import java.net.URL;
import java.util.ArrayList;

import org.opensextant.regex.RegexAnnotation;
import org.opensextant.regex.RegexMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CreoleResource(name = "OpenSextant Regex Finder", comment = "A simple plugin that finds and normalizes entities "
    + "based on Java regular expresssions")
public class RegexFinderPR extends AbstractLanguageAnalyser implements ProcessingResource {
  private static final long serialVersionUID = 1375472181851584128L;
  // the Regexmatcher object which does all of the work
  private RegexMatcher reger;
  // the annotationSet into which the dates will be written
  private String outputAnnotationSet;
  // the file containing the patterns
  private URL patternFile = null;
  // the log
  static Logger log = LoggerFactory.getLogger(RegexFinderPR.class);

  /**
   * Initializes the DateFinderPR resource.
   */
  private void initialize() {
    // initialize the regex matcher
    reger = new RegexMatcher(patternFile);
  } // end initialize

  /**
   * @return
   * @throws ResourceInstantiationException
   */
  @Override
  public Resource init() throws ResourceInstantiationException {
    this.initialize();
    return this;
  }

  /**
   * @throws ResourceInstantiationException
   */
  @Override
  public void reInit() throws ResourceInstantiationException {
    this.initialize();
  }

  /**
   * @throws ExecutionException
   */
  @Override
  public void execute() throws ExecutionException {
    // get the annotation set into which we will place any annotations found
    AnnotationSet annotSet = (outputAnnotationSet == null || outputAnnotationSet.equals("")) ? document
        .getAnnotations() : document.getAnnotations(outputAnnotationSet);
    // get the text of the document
    String text = getDocument().getContent().toString();
    // find the matches via the regex matcher
    ArrayList<RegexAnnotation> matches = reger.match(text);
    // loop over all the results
    for (RegexAnnotation a : matches) {
      // fill in all the annotation features
      FeatureMap feats = Factory.newFeatureMap();
      feats.putAll(a.getFeatures());
      // create the GATE annotation
      try {
        annotSet.add((long) a.getStart(), (long) a.getEnd(), a.getType(), feats);
      } catch (InvalidOffsetException e) {
        log.error("Invalid Offset exception when creating  annotation", e);
      }
    }
  }

  /**
   * @return
   */
  public String getOutputAnnotationSet() {
    return outputAnnotationSet;
  }

  /**
   * @param outputAnnotationSet
   */
  @Optional
  @RunTime
  @CreoleParameter
  public void setOutputAnnotationSet(String outputAnnotationSet) {
    this.outputAnnotationSet = outputAnnotationSet;
  }

  /**
   * @return
   */
  public URL getPatternFile() {
    return patternFile;
  }

  /**
   * @param patternFile
   */
  @CreoleParameter
  public void setPatternFile(URL patternFile) {
    this.patternFile = patternFile;
  }
} // class DateFinderPR
