/*
                  NOTICE
This software was produced for the U. S. Government
under Contract No. W15P7T-11-C-F600, and is
subject to the Rights in Noncommercial Computer Software
and Noncommercial Computer Software Documentation
Clause 252.227-7014 (JUN 1995)
Copyright 2010 The MITRE Corporation. All Rights Reserved.
 */
package org.opensextant.toolbox;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Controller;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.opensextant.placedata.PlaceCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This PR is used as a diagnostic tool. It collects a histogram (word counts) of how many times a specific candidate
 * place name was determined to be a Place, not a place or no opinion. *
 */
@CreoleResource(name = "OpenSextant PlaceNameStatisticsPR", comment = "Diagnostic tool for collecting a histogram"
    + " of Place name results")
public class PlaceNameStatisticsPR extends AbstractLanguageAnalyser implements ProcessingResource, ControllerAwarePR {
  private static final long serialVersionUID = 1L;
  private File outputDir;
  private String outFileName = "placeNameStats.txt";
  private File vocabFile;
  transient BufferedWriter vocabWriter;
  /** Map is organized as placename -> [PlaceCount,NotPlaceCount,NoOpinioCount]. */
  transient Map<String, Long[]> placeNameStats = new HashMap<String, Long[]>();
  /** A running count of how many documents seen so far. */
  private Integer docCount = 0;
  String placeAnnotationName = "placecandidate";
  String notPlaceAnnotationName = "NOT_place";
  String noOpionPlaceAnnotationName = "NO_OPINION_place";
  String featureName = "placeCandidate";
  String assessName = "Assessment";
  Boolean convertToLower;
  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(PlaceNameStatisticsPR.class);

  private void initialize() {
    LOGGER.info("Initializing ");
    docCount = 0;
    openFiles();
    placeNameStats.clear();
  }

  /** Do the initialization. */
  @Override
  public Resource init() throws ResourceInstantiationException {
    initialize();
    return this;
  }

  /** Re-do the initialization. */
  @Override
  public void reInit() throws ResourceInstantiationException {
    initialize();
  }

  /** Do the work. */
  @Override
  public void execute() throws ExecutionException {
    // get all of the annotations of interest
    AnnotationSet placeCandAnnoSet = document.getAnnotations().get(this.placeAnnotationName);
    docCount++;
    LOGGER.info("(" + docCount + ") " + document.getName() + " has " + placeCandAnnoSet.size() + " "
        + placeAnnotationName + " annotations");
    // loop over all placeCandidate annotations
    for (Annotation anno : placeCandAnnoSet) {
      PlaceCandidate pc = (PlaceCandidate) anno.getFeatures().get(featureName);
      // get the name as found in the document
      String placename = gate.Utils.cleanStringFor(document, anno);
      // convert to lower case if specified
      if (convertToLower) {
        placename = placename.toLowerCase();
      }
      // if name not previously seen, add entry to stats Map
      if (!placeNameStats.containsKey(placename)) {
        Long[] tmp = {0L, 0L, 0L, 0L, 0L, 0L, 0L};
        placeNameStats.put(placename, tmp);
      }
      // get the confidence score
      double score = pc.getPlaceConfidenceScore();

      String assess = (String) anno.getFeatures().get(assessName);

      // increment stats based on the confidence score
      Long[] tmp = placeNameStats.get(placename);
      // if is a place
      if (score > 0.0) {
        tmp[0]++;
      }
      // if is not a place
      if (score < 0.0) {
        tmp[1]++;
      }
      // if no opinion
      if (Math.abs(score) < 0.0001) {
        tmp[2]++;
      }

      if (assess != null && assess.startsWith("TP")) {
        tmp[3]++;
      }

      if (assess != null && assess.startsWith("TN")) {
        tmp[4]++;
      }

      if (assess != null && assess.startsWith("FP")) {
        tmp[5]++;
      }

      if (assess != null && assess.startsWith("FN")) {
        tmp[6]++;
      }

      placeNameStats.put(placename, tmp);
    } // end placeCandidate annotation loop
      // write out interim stats files
    if (docCount % 500 == 0) {
      LOGGER.info("Writing incremental stats at " + docCount + " documents");
      closeFiles();
      openFiles();
      writeStats();
    }
  }
  /** End execute. */

  @Override
  public void controllerExecutionAborted(Controller arg0, Throwable arg1) throws ExecutionException {
    closeFiles();
    openFiles();
    writeStats();
    placeNameStats.clear();
    closeFiles();
  }

  @Override
  public void controllerExecutionFinished(Controller arg0) throws ExecutionException {
    closeFiles();
    openFiles();
    writeStats();
    placeNameStats.clear();
    closeFiles();
  }

  @Override
  public void controllerExecutionStarted(Controller arg0) throws ExecutionException {
    initialize();
  }

  private void openFiles() {
    vocabFile = new File(outputDir, this.outFileName);
    try {
      vocabWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(vocabFile), "UTF-8"));
    } catch (UnsupportedEncodingException e1) {
      LOGGER.error("Couldnt write to " + vocabFile.getName(), e1);
      return;
    } catch (FileNotFoundException e1) {
      LOGGER.error("Couldnt write to " + vocabFile.getName(), e1);
      return;
    }
    // write header
    try {
      vocabWriter.write("word\tPlaceCount\tNotPlaceCount\tNoOpinionCount\tTP\tTN\tFP\tFN\tTotalCount\tPlacePercentage");
      vocabWriter.newLine();
    } catch (IOException e) {
      LOGGER.error("Couldnt write to " + vocabFile.getName(), e);
    }
  }

  private void closeFiles() {
    // flush and close all the writers
    try {
      vocabWriter.flush();
      vocabWriter.close();
    } catch (IOException e) {
      LOGGER.error("Couldnt close " + vocabFile.getName(), e);
    }
  }

  private void writeStats() {
    LOGGER.info("Place name stats has " + placeNameStats.size() + " entries");
    // write out vocab stats
    for (String word : placeNameStats.keySet()) {
      Long[] count = placeNameStats.get(word);
      Long total = count[0] + count[1] + count[2];
      double ratio = count[0] / (1.0 * total);
      try {
        vocabWriter.write(word);
        vocabWriter.write("\t");
        vocabWriter.write(count[0].toString());
        vocabWriter.write("\t");
        vocabWriter.write(count[1].toString());
        vocabWriter.write("\t");
        vocabWriter.write(count[2].toString());
        vocabWriter.write("\t");
        vocabWriter.write(count[3].toString());
        vocabWriter.write("\t");
        vocabWriter.write(count[4].toString());
        vocabWriter.write("\t");
        vocabWriter.write(count[5].toString());
        vocabWriter.write("\t");
        vocabWriter.write(count[6].toString());
        vocabWriter.write("\t");
        vocabWriter.write(total.toString());
        vocabWriter.write("\t");
        vocabWriter.write(Double.toString(ratio));
        vocabWriter.newLine();
      } catch (IOException e) {
        LOGGER.error("Couldnt write to " + vocabFile.getName(), e);
      }
    }
    try {
      vocabWriter.flush();
    } catch (IOException e) {
      LOGGER.error("Error when flushing writer", e);
    }
  }

  public File getOutputDir() {
    return outputDir;
  }

  @CreoleParameter(defaultValue = "C:\\dump\\vocab")
  public void setOutputDir(File outputDir) {
    if(outputDir.mkdirs()){
      this.outputDir = outputDir;
    }else{
      LOGGER.error("Could not create output directory" + outputDir.getPath());
    }
  }

  public String getOutFileName() {
    return outFileName;
  }

  @CreoleParameter(defaultValue = "placeNameStats.txt")
  public void setOutFileName(String outFileName) {
    this.outFileName = outFileName;
  }

  public Boolean getLower() {
    return convertToLower;
  }

  @RunTime
  @CreoleParameter(defaultValue = "true")
  public void setLower(Boolean lower) {
    this.convertToLower = lower;
  }
} // class NaiveTaggerPR
