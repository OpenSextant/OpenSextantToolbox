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

import java.util.List;

import org.opensextant.matching.MatcherFactory;
import org.opensextant.matching.PlacenameMatcher;
import org.opensextant.placedata.PlaceCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * A Solr-based ProcessingResource that tags mentions of geospatial candidates found in a dcoument.
 * @author David Smiley, MITRE, dsmiley@mitre.org
 * @author Marc Ubaldino, MITRE, ubaldino@mitre.org *
 */
@CreoleResource(name = "OpenSextant NaiveTaggerSolr", comment = "A Solr-based tagger")
public class NaiveTaggerSolrPR extends AbstractLanguageAnalyser implements ProcessingResource {
  /**
     *
     */
  private static final long serialVersionUID = -6167312014577862928L;
  // Log object
  private static final Logger LOGGER = LoggerFactory.getLogger(NaiveTaggerSolrPR.class);
  private transient PlacenameMatcher matcher;
  private String outputASName;
  private String annotationType;

  private boolean outputASNameSet = false;

  // location of solr gazetteer
  private String gazetteerHome;

  // The parameters passed in by the user
  String inputASName; // The name of the input AnnotationSet
  Boolean tagAbbreviations; // tag placenames which are abreviations or codes
  // TODO expose calibrate and calibrateScore as PR parameters
  // to force all confidences to calibrateScore for calibration
  boolean calibrate = false;
  double calibrateScore = 0.0;

  /**
   * @return gate_resource
   * @throws ResourceInstantiationException
   */
  @Override
  public Resource init() throws ResourceInstantiationException {
    super.init();

    // Check to see if Matcherfactory has not been configured
    if (!MatcherFactory.isConfigured()) {

      // no gazeetteer home given, try to use default
      if (gazetteerHome == null || gazetteerHome.length() == 0) {
        LOGGER.info("No gazetter home given for MatcherFactory, trying default config");
        MatcherFactory.config("");
        // if the the default worked, start
        if (MatcherFactory.isConfigured()) {
          MatcherFactory.start();
          LOGGER.info("NaiveTagger is using MatcherFactory configured to use " + MatcherFactory.getHomeLocation());
        } else {
          LOGGER.error("No config given and no default set");
          return this;
        }
      } else {
        MatcherFactory.config(gazetteerHome);
        MatcherFactory.start();
      }
    } else {
      // already configured
      if (!MatcherFactory.isStarted()) {
        MatcherFactory.start();
      }
      LOGGER.info("NaiveTagger is using MatcherFactory configured to use " + MatcherFactory.getHomeLocation());
    }

    // see if the Factory is running
    if (!MatcherFactory.isStarted()) {
      LOGGER.error("MatcherFactory did not start. Not configured?");
      return this;
    }

    matcher = MatcherFactory.getMatcher();
    if (matcher == null) {
      LOGGER.error("Could not get a matcher from MatcherFactory. Not configured?");
      return this;
    }
    matcher.tagAbbreviations(tagAbbreviations);
    return this;
  }

  /**
     *
     */
  @Override
  public void cleanup() {
    super.cleanup();
    if (matcher != null) {
      matcher.cleanup();
    }
  }

  /**
  
   */
  @Override
  public void execute() throws ExecutionException {
    if (matcher == null) {
      throw new IllegalStateException("This PR hasn't been init'ed!");
    }
    List<PlaceCandidate> matches = null;
    try {
      matches = matcher.matchText(document.getContent().toString(), document.getName());
    } catch (Exception err) {
      LOGGER.error("Error when tagging document " + document.getName(), err);
      return;
    }
    // If no output Annotation set was given, append to the input AS
    AnnotationSet annotSet = outputASNameSet ? document.getAnnotations(outputASName) : document.getAnnotations();

    for (PlaceCandidate pc : matches) {
      // create and populate the PlaceCandidate annotation
      FeatureMap feats = Factory.newFeatureMap();
      feats.put("string", pc.getPlaceName());
      feats.put("placeCandidate", pc);
      if (calibrate) {
        pc.setPlaceConfidenceScore(calibrateScore);
      }
      try {
        annotSet.add(pc.getStart(), pc.getEnd(), annotationType, feats);
      } catch (InvalidOffsetException offsetErr) {
        LOGGER.error("Error when adding PlaceCandidate to document in " + document.getName(), offsetErr);
      }
    }
  }

  /**
   * @return
   */
  public String getInputASName() {
    return inputASName;
  }

  /**
   * @param inputASName
   */
  @Optional
  @RunTime
  @CreoleParameter
  public void setInputASName(String inputASName) {
    this.inputASName = inputASName;
  }

  /**
   * @return
   */
  public String getOutputASName() {
    return outputASName;
  }

  /**
   * @return
   */
  public String getAnnotationType() {
    return annotationType;
  }

  /**
   * @param annotationType
   */
  @Optional
  @RunTime
  @CreoleParameter(defaultValue = "placecandidate")
  public void setAnnotationType(String annotationType) {
    this.annotationType = annotationType;
  }

  public String getGazHome() {
    return gazetteerHome;
  }

  @CreoleParameter
  public void setGazHome(String gazHome) {
    this.gazetteerHome = gazHome;
  }

  /**
   * @param outputASName
   */
  @Optional
  @RunTime
  @CreoleParameter
  public void setOutputASName(String outputASName) {
    this.outputASName = outputASName;
    outputASNameSet = (outputASName != null && !outputASName.isEmpty());
  }

  public Boolean getTagAbbreviations() {
    return tagAbbreviations;
  }

  @Optional
  @CreoleParameter(defaultValue = "false")
  public void setTagAbbreviations(Boolean tagAbbreviations) {
    this.tagAbbreviations = tagAbbreviations;
  }
}
