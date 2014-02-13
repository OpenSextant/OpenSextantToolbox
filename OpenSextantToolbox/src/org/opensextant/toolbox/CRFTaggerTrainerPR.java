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

import gate.Controller;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * * This GATE Processing Resource (PR) is a wrapper around the JCarafe Conditional Random Fields (CRF) engine. This PR
 * supports the creation (training) of CRF models by generating documents in the form needed by the CRF Tagger. NOTE:
 * Current version does not actual perform training, just creates inputs for training. Training is actually done
 * offline.
 */
@CreoleResource(name = "OpenSextant CRF TaggerTrainer", comment = "Utility PR that creates inputs for a JCarafe tagger training session ")
public class CRFTaggerTrainerPR extends AbstractLanguageAnalyser implements ProcessingResource, ControllerAwarePR {

  /**
   *
   */
  private static final long serialVersionUID = 4706195501886387500L;

  // The parameters passed in by the user
  // the input AnnotationSet
  String inputASName;

  // file path to the directory containing the skeleton job
  URL jobTemplateURL;
  File jobTemplateDir;

  // file path to the directory where output will be placed
  URL jobDirectoryURL;
  File jobDirectory;

  // file path to directory will output docs will be placed
  File docsDirectory;

  // file for the tags to be trained on
  File tagsetFile;

  // file for the feature specifications to use in training
  File fspecFile;

  // a list of Token feature names to send to JCarafe
  List<String> tokenFeaturesToSend;

  // a list of Annotation names to include in output docs null = none, "*"=all
  List<String> annosToSend;

  // Log object
  static Logger log = LoggerFactory.getLogger(CRFTaggerTrainerPR.class);

  private void initialize() {

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
    // create a Json document from GATE document

    // create a MAT JSON string from the GATE document
    String jsonToWrite = DocumentFactory.jsonfromGATETokensAndAnnos(document, inputASName, tokenFeaturesToSend,
        annosToSend);
    String docName = document.getName();

    File out = new File(docsDirectory, docName + ".json");

    if (!out.exists()) {
      try {
        FileUtils.writeStringToFile(out, jsonToWrite, "UTF-8");
      } catch (IOException e) {
        log.error("Could not write json string to file:" + out.getAbsolutePath() + " error was:" + e.getMessage());
      }
    } else {
      // TODO write doc under other name
      log.error("Name Collision" + docName);
    }

  } // end execute

  public String getInputASName() {
    return inputASName;
  }

  @Optional
  @RunTime
  @CreoleParameter
  public void setInputASName(String inputASName) {
    this.inputASName = inputASName;
  }

  public URL getJobTemplateURL() {
    return jobTemplateURL;
  }

  @CreoleParameter
  public void setJobTemplateURL(URL jobTemplateURL) {
    this.jobTemplateURL = jobTemplateURL;
    try {
      this.jobTemplateDir = new File(jobTemplateURL.toURI());
    } catch (URISyntaxException e) {
      log.error("Could not create file from " + jobTemplateURL.toString());
    }
  }

  public URL getJobDirectoryURL() {
    return jobDirectoryURL;
  }

  @CreoleParameter
  public void setJobDirectoryURL(URL jobDirectoryURL) {
    this.jobDirectoryURL = jobDirectoryURL;
    try {
      this.jobDirectory = new File(jobDirectoryURL.toURI());
    } catch (URISyntaxException e) {
      log.error("Could not create file from " + jobDirectoryURL.toString());
    }
  }

  public List<String> getTokenFeaturesToSend() {
    return tokenFeaturesToSend;
  }

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

  public List<String> getAnnosToSend() {
    return annosToSend;
  }

  @Optional
  @RunTime
  @CreoleParameter
  public void setAnnosToSend(List<String> annosToSend) {
    this.annosToSend = annosToSend;
  }

  @Override
  public void controllerExecutionStarted(Controller arg0) throws ExecutionException {

    // ensure job dir exists
    jobDirectory.mkdirs();

    // populate job directory with contents of template job
    try {
      FileUtils.copyDirectory(jobTemplateDir, jobDirectory);
    } catch (IOException e) {
      log.error("Could not create job:" + e.getMessage());
    }

    docsDirectory = new File(jobDirectory, "docs");
    tagsetFile = new File(jobDirectory, "tagset.txt");
    fspecFile = new File(jobDirectory, "features.fspec");

  }

  @Override
  public void controllerExecutionFinished(Controller arg0) throws ExecutionException {
    String nl = System.getProperty("line.separator");

    // write out the annotations names to the "tagset.txt" file, one per line
    try {
      FileUtils.writeStringToFile(tagsetFile, nl, true);
      for (String at : annosToSend) {
        FileUtils.writeStringToFile(tagsetFile, at + nl, true);
      }
    } catch (IOException e) {
      log.error("Could not write tagset to file:" + tagsetFile.getAbsolutePath() + " error was:" + e.getMessage());
    }

    // write out any token features to the feature spec file as attributeFn(att) specs
    try {
      FileUtils.writeStringToFile(fspecFile, nl, true);
      for (String f : tokenFeaturesToSend) {
        String tmp = f + "Fn" + "\tas\tattributeFn(" + f + ");" + nl;
        FileUtils.writeStringToFile(fspecFile, tmp, true);
      }
    } catch (IOException e) {
      log.error("Could not write to feature specs to file:" + fspecFile.getAbsolutePath() + " error was:"
          + e.getMessage());

    }

  }

  @Override
  public void controllerExecutionAborted(Controller arg0, Throwable arg1) throws ExecutionException {

  }

}
