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
import gate.Utils;
import gate.annotation.AnnotationSetImpl;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opensextant.placedata.PlaceCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This PR is used as a diagnostic tool. It collects a histogram (word counts) of how many times a specific candidate
 * place name was determined to be a Place, not a place or no opinion. *
 */
@CreoleResource(name = "OpenSextant PlaceNameRuleDumpPR", comment = "Diagnostic tool for analyzing on"
    + " PlaceCandidate annotations")
public class PlaceNameRuleDumpPR extends AbstractLanguageAnalyser implements ProcessingResource, ControllerAwarePR {
  private static final long serialVersionUID = 1L;
  private File outputDir;
  private String outFileName = "placeNameStatsWithRules.txt";
  private File vocabFile;
  transient BufferedWriter vocabWriter;
  /** A running count of how many documents seen so far. */
  private Integer docCount = 0;
  String placeAnnotationName = "placecandidate";
  String featureName = "placeCandidate";
  String assessName = "Assessment";
  Long contxtSize = 75L;
  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(PlaceNameRuleDumpPR.class);

  private void initialize() {
    LOGGER.info("Initializing ");
    docCount = 0;
    openFiles();
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
    AnnotationSet truthAnnoSet = document.getAnnotations("Key").get("PLACE");
    docCount++;
    LOGGER.info("(" + docCount + ") " + document.getName() + " has " + placeCandAnnoSet.size() + " " + placeAnnotationName + " annotations");
    LOGGER.info("(" + docCount + ") " + document.getName() + " has " + truthAnnoSet.size() + " " + "Truth" + " annotations");


    // loop over all placeCandidate annotations
    for (Annotation anno : placeCandAnnoSet) {
      anno.getFeatures().put(assessName, "UNK");
      calcAssessment(anno, truthAnnoSet);
      writeStats(anno);
    } // end placeCandidate annotation loop

    // loop over all truth annotations
    Set<String> featureNames = new HashSet<String>();
    featureNames.add("MATCHED");
    AnnotationSet matchedTruth = truthAnnoSet.get("PLACE", featureNames);

    for (Annotation anno : truthAnnoSet) {
      if (!matchedTruth.contains(anno)) {
        writeFN(anno);
      }

    } // end placeCandidate annotation loop

  } /** End execute. */

  private void calcAssessment(Annotation anno, AnnotationSet truthAnnoSet) {

    // get the PlaceCandidate obj
    PlaceCandidate pc = (PlaceCandidate) anno.getFeatures().get(featureName);
    // get the confidence score
    double score = pc.getPlaceConfidenceScore();

    AnnotationSetImpl truth = (AnnotationSetImpl) truthAnnoSet;
    long start = anno.getStartNode().getOffset();
    long end = anno.getEndNode().getOffset();
    AnnotationSet exactSet = truth.getStrict(start, end);
    AnnotationSet containedSet = truth.getContained(start, end);
    AnnotationSet coveredSet = truth.getCovering("PLACE", start, end);
    AnnotationSet overlapSet = truth.get("PLACE", start, end);

    boolean isPlace = score > 0;

    if (exactSet.size() == 1  ) {

      if (isPlace) {
        anno.getFeatures().put(assessName, "TP");
      } else {
        anno.getFeatures().put(assessName, "FN-exact");
      }
      Annotation exactAnno = exactSet.iterator().next();
      exactAnno.getFeatures().put("MATCHED", true);

    }

    if (exactSet.size() != 1 && (!containedSet.isEmpty() || !coveredSet.isEmpty()   || !overlapSet.isEmpty() ) ){

      if (isPlace) {
        anno.getFeatures().put(assessName, "TP-Overlap");
      } else {
        anno.getFeatures().put(assessName, "FN-Overlap");
      }

      for (Annotation a : containedSet) {
        a.getFeatures().put("MATCHED", true);
      }

      for (Annotation a : coveredSet) {
        a.getFeatures().put("MATCHED", true);
      }

      for (Annotation a : overlapSet) {
        a.getFeatures().put("MATCHED", true);
      }

    }

    if (exactSet.isEmpty() && containedSet.isEmpty() && coveredSet.isEmpty() && overlapSet.isEmpty()) {

      if(isPlace){
        anno.getFeatures().put(assessName, "FP");
      }else{
        anno.getFeatures().put(assessName, "TN");
      }

    }


  }

  @Override
  public void controllerExecutionAborted(Controller arg0, Throwable arg1) throws ExecutionException {
    closeFiles();
  }

  @Override
  public void controllerExecutionFinished(Controller arg0) throws ExecutionException {
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
      LOGGER.error("Couldnt open  " + vocabFile.getName(), e1);
      return;
    } catch (FileNotFoundException e1) {
      LOGGER.error("Couldnt open  " + vocabFile.getName(), e1);
      return;
    }
    // write header
    try {
      vocabWriter.write("placeName\tAssessment\tStart\tEnd\tConfidence\tRules\tRuleWeights\tContext\tDocument");
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
      LOGGER.error("Couldnt close  " + vocabFile.getName(), e);
    }
  }

  private void writeStats(Annotation anno) {
    // get the PlaceCandidate obj
    PlaceCandidate pc = (PlaceCandidate) anno.getFeatures().get(featureName);
    // get the name as found in the document
    String placename = gate.Utils.cleanStringFor(document, anno);
    // get the confidence score
    double score = pc.getPlaceConfidenceScore();
    // get the rules
    List<String> rules = pc.getRules();
    // get the confidences
    List<Double> scores = pc.getConfidences();
    Long start = pc.getStart();
    Long end = pc.getEnd();
    String context = getContext(anno);

    String assessment = (String) anno.getFeatures().get(assessName);

    try {
      vocabWriter.write(placename);
      vocabWriter.write("\t");
      vocabWriter.write(assessment);
      vocabWriter.write("\t");
      vocabWriter.write(start.toString());
      vocabWriter.write("\t");
      vocabWriter.write(end.toString());
      vocabWriter.write("\t");
      vocabWriter.write(Double.toString(score));
      vocabWriter.write("\t");
      vocabWriter.write(rules.toString());
      vocabWriter.write("\t");
      vocabWriter.write(scores.toString());
      vocabWriter.write("\t");
      vocabWriter.write(context);
      vocabWriter.write("\t");
      vocabWriter.write(document.getName());
      vocabWriter.newLine();
    } catch (IOException e) {
      LOGGER.error("Couldnt write to " + vocabFile.getName(), e);
    }
    try {
      vocabWriter.flush();
    } catch (IOException e) {
      LOGGER.error("Error when flushing writer", e);
    }
  }

  private void writeFN(Annotation anno) {

    // get the name as found in the document
    String placename = gate.Utils.cleanStringFor(document, anno);
    // get the confidence score
    double score = 0;
    // get the rules
    List<String> rules = new ArrayList<String>();
    // get the confidences
    List<Double> scores = new ArrayList<Double>();
    Long start = anno.getStartNode().getOffset();
    Long end = anno.getEndNode().getOffset();
    String context = getContext(anno);

    String assessment = "FN-NoPC";

    try {
      vocabWriter.write(placename);
      vocabWriter.write("\t");
      vocabWriter.write(assessment);
      vocabWriter.write("\t");
      vocabWriter.write(start.toString());
      vocabWriter.write("\t");
      vocabWriter.write(end.toString());
      vocabWriter.write("\t");
      vocabWriter.write(Double.toString(score));
      vocabWriter.write("\t");
      vocabWriter.write(rules.toString());
      vocabWriter.write("\t");
      vocabWriter.write(scores.toString());
      vocabWriter.write("\t");
      vocabWriter.write(context);
      vocabWriter.write("\t");
      vocabWriter.write(document.getName());
      vocabWriter.newLine();
    } catch (IOException e) {
      LOGGER.error("Couldnt write to " + vocabFile.getName(), e);
    }
    try {
      vocabWriter.flush();
    } catch (IOException e) {
      LOGGER.error("Error when flushing writer", e);
    }
  }

  private String getContext(Annotation anno) {
    Long candStart = anno.getStartNode().getOffset();
    Long candEnd = anno.getEndNode().getOffset();
    Long contextStart = candStart - contxtSize;
    Long contextEnd = candEnd + contxtSize;
    if (contextStart < 0) {
      contextStart = 0L;
    }
    if (contextEnd > Utils.lengthLong(document)) {
      contextEnd = Utils.lengthLong(document);
    }
    return Utils.cleanStringFor(document, contextStart, contextEnd);
  }

  public File getOutputDir() {
    return outputDir;
  }

  @CreoleParameter(defaultValue = "C:\\dump\\vocab")
  public void setOutputDir(File outputDir) {
    outputDir.mkdirs();
    this.outputDir = outputDir;
  }

  public String getOutFileName() {
    return outFileName;
  }

  @CreoleParameter(defaultValue = "placeNameStatsWithRules.txt")
  public void setOutFileName(String outFileName) {
    this.outFileName = outFileName;
  }
} // class NaiveTaggerPR
