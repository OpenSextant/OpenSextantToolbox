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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.mitre.jcarafe.jarafe.JarafeTagger;
import org.opensextant.mat.Annot;
import org.opensextant.mat.Aset;
import org.opensextant.mat.MATDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * * This GATE Processing Resource (PR) is a wrapper around the JCarafe Conditional Random Fields (CRF) engine. This PR
 * allows the contents and designated GATE elements (Token features) to be used as input to a JCarafe model and the
 * results added to the GATE document.
 */
@CreoleResource(name = "OpenSextant CRF Tagger", comment = "An implementation of a Conditional Random Fields Tagger based on JCarafe")
public class CRFTaggerPR extends AbstractLanguageAnalyser implements ProcessingResource {

  /**
   *
   */
  private static final long serialVersionUID = 4706195501886387500L;

  // The parameters passed in by the user
  // the input and output AnnotationSets
  String inputASName;
  String outputASName;

  // file path to the JCarafe model to be used
  URL modelPath;

  // a list of Token feature names to send to JCarafe
  List<String> tokenFeaturesToSend;

  // a list of Annotation names to merge back into GATE document, null = none, "*"=all
  List<String> annosToMerge;

  // send tokens?
  Boolean sendTokens;

  // the CRF tagger
  JarafeTagger tagger = new JarafeTagger();

  // Log object
  static Logger log = LoggerFactory.getLogger(CRFTaggerPR.class);

  private void initialize() {
    // the fixed invocation params
    String filePath = "";
    try {
      filePath = new File(modelPath.toURI()).getAbsolutePath();
    } catch (URISyntaxException e) {
      log.error("Cannot convert " + modelPath + " to file path");
    }
    log.debug("Using modelpath =" + filePath);

    List<String> params = new ArrayList<String>();
    params.add("--model");
    params.add(filePath);
    params.add("--mode");
    params.add("json");

    if (sendTokens) {
      params.add("--no-pre-proc");
    }// else {
    params.add("--keep-tokens");
    // }

    tagger.initialize(params.toArray(new String[1]));
    // tagger.initializeAsJson(jobDirectory);

  }

  /**
   * @return
   * @throws ResourceInstantiationException
   */
  @Override
  public Resource init() throws ResourceInstantiationException {
    initialize();
    return this;
  }

  /**
   * @throws ResourceInstantiationException
   */
  @Override
  public void reInit() throws ResourceInstantiationException {
    initialize();
  }

  /**
   * @throws ExecutionException
   */
  @Override
  public void execute() throws ExecutionException {
    // If no Annotation set name was given, use the default AnnotationSet
    AnnotationSet outputAS = (outputASName == null || outputASName.equals("")) ? document.getAnnotations() : document
        .getAnnotations(outputASName);

    // create a MAT JSON string from the GATE document
    String jsonToSend = "";
    if (sendTokens) {
      jsonToSend = DocumentFactory.jsonfromGATETokens(document, inputASName, tokenFeaturesToSend);
    } else {
      jsonToSend = DocumentFactory.jsonfromGATEContent(document);
    }

    // send json to tagger
    log.debug("Sending " + jsonToSend);
    String jsonBack = tagger.processString(jsonToSend);
    log.debug("Received " + jsonBack);

    // merge the desired results back into GATE document
    merge(jsonBack, outputAS, annosToMerge);

  } // end execute

  private void merge(String jsonBack, AnnotationSet outputAS, List<String> toMerge) {

    if (toMerge == null || toMerge.isEmpty()) {
      return;
    }

    boolean mergeAll = false;
    if (toMerge.size() == 1 && toMerge.get(0).equals("*")) {
      mergeAll = true;
    }

    MATDocument mat = DocumentFactory.matfromJSONString(jsonBack);

    // pull the desired annots
    List<Aset> asets = mat.getAsets();
    for (Aset aset : asets) {
      String tmpType = aset.getType();
      // if its one we want to merge
      if (mergeAll || toMerge.contains(tmpType)) {
        // get all the Annots
        List<Annot> annots = aset.getAnnots();
        // create a GATE annotation for each
        for (Annot a : annots) {
          Long start = a.getStart();
          Long end = a.getEnd();
          FeatureMap fm = gate.Factory.newFeatureMap();
          // add attrs here

          try {
            log.debug("Adding a " + tmpType + " at " + start + "," + end);
            outputAS.add(start, end, tmpType, fm);
          } catch (InvalidOffsetException e) {
            log.error("Invalid offset when insert new annotation:" + e.getMessage());
          }
        }
      } else {
        log.debug("Not merging annotations of type " + tmpType);
      }
    }
  }

  public URL getModelPath() {
    return modelPath;
  }

  @CreoleParameter
  public void setModelPath(URL modelPath) {
    this.modelPath = modelPath;
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

  public String getOutputASName() {
    return outputASName;
  }

  /**
   * @param inputASName
   */
  @Optional
  @RunTime
  @CreoleParameter
  public void setOutputASName(String outputASName) {
    this.outputASName = outputASName;
  }

  public List<String> getTokenFeaturesToSend() {
    return tokenFeaturesToSend;
  }
  /**
   * @param inputASName
   */
  @Optional
  @RunTime
  @CreoleParameter
  public void setTokenFeaturesToSend(List<String> tokenFeaturesToSend) {
    if (tokenFeaturesToSend == null) {
      this.tokenFeaturesToSend = new ArrayList<String>();
    } else {
      this.tokenFeaturesToSend = tokenFeaturesToSend;
    }

  }

  public List<String> getAnnosToMerge() {
    return annosToMerge;
  }

  /**
   * @param inputASName
   */
  @Optional
  @RunTime
  @CreoleParameter(defaultValue = "*")
  public void setAnnosToMerge(List<String> annosToMerge) {
    if (annosToMerge == null) {
      this.annosToMerge = new ArrayList<String>();
    } else {
      this.annosToMerge = annosToMerge;
    }

  }

  public Boolean getSendTokens() {
    return sendTokens;
  }

  @CreoleParameter(defaultValue = "true")
  public void setSendTokens(Boolean sendTokens) {
    this.sendTokens = sendTokens;
  }

}
