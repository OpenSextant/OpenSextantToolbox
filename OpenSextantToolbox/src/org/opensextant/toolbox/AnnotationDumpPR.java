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
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.Resource;
import gate.Utils;
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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This PR is used as a diagnostic tool. It outputs an annotation and selected features of that annotation as a simple
 * delimited file.
 */
@CreoleResource(name = "OpenSextant AnnotationDumpPR", comment = "Diagnostic tool for dumping annotations")
public class AnnotationDumpPR extends AbstractLanguageAnalyser implements ProcessingResource, ControllerAwarePR {

  private static final long serialVersionUID = 1L;
  private File outputDir;
  private String outFileName = "annotations.txt";
  private File vocabFile;
  transient BufferedWriter vocabWriter;
  /** A running count of how many documents seen so far. */
  private Integer docCount = 0;
  String annotationSetName;
  String annotationName = "Token";
  transient List<String> featureNames = new ArrayList<String>();
  Long contxtSize = 75L;
  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationDumpPR.class);

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
    // get all of the annotations of interest from the annotationset given
    AnnotationSet annoSet = document.getAnnotations(annotationSetName).get(annotationName);
    docCount++;
    LOGGER.info("(" + docCount + ") " + document.getName() + " has " + annoSet.size() + " " + annotationName
        + " annotations");
    // loop over all selected annotations
    for (Annotation anno : annoSet) {
      writeAnnotation(anno);
    } // end annotation loop
  }
  /** End execute. */

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
      LOGGER.error("Couldnt open " + vocabFile.getName(), e1);
      return;
    } catch (FileNotFoundException e1) {
      LOGGER.error("Couldnt open " + vocabFile.getName(), e1);
      return;
    }
    // write header
    try {
      vocabWriter.write("Annotation");
      vocabWriter.write("\t");
      for (String name : featureNames) {
        vocabWriter.write(name);
        vocabWriter.write("\t");
      }
      vocabWriter.write("Start");
      vocabWriter.write("\t");
      vocabWriter.write("End");
      vocabWriter.write("\t");
      vocabWriter.write("Snippet");
      vocabWriter.write("\t");
      vocabWriter.write("Document");
      vocabWriter.newLine();
    } catch (IOException e) {
      LOGGER.error("Couldnt write to " + vocabFile.getName(), e);
      return;
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

  private void writeAnnotation(Annotation anno) {
    // get the annotation as found in the document
    String annoString = gate.Utils.cleanStringFor(document, anno);
    String context = getContext(anno);
    try {
      vocabWriter.write(annoString);
      vocabWriter.write("\t");
      FeatureMap feats = anno.getFeatures();
      for (String name : featureNames) {
        Object tmp = feats.get(name);
        if (tmp != null) {
          vocabWriter.write(tmp.toString());
        } else {
          vocabWriter.write("");
        }
        vocabWriter.write("\t");
      }
      vocabWriter.write(anno.getStartNode().getOffset().toString());
      vocabWriter.write("\t");
      vocabWriter.write(anno.getEndNode().getOffset().toString());
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
      LOGGER.error("Error when flushing " + vocabFile.getName(), e);
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

    if(outputDir.mkdirs()){
      this.outputDir = outputDir;
    }else{
      LOGGER.error("Could not create output directory" + outputDir.getPath());
    }

  }

  public String getOutFileName() {
    return outFileName;
  }

  @CreoleParameter(defaultValue = "annotations.txt")
  public void setOutFileName(String outFileName) {
    this.outFileName = outFileName;
  }

  public List<String> getFeatureNames() {
    return featureNames;
  }

  @RunTime
  @CreoleParameter
  public void setFeatureNames(List<String> featureNames) {
    this.featureNames = featureNames;
  }

  public String getAnnotationSetName() {
    return annotationSetName;
  }

  @RunTime
  @CreoleParameter(defaultValue = "")
  public void setAnnotationSetName(String annotationSetName) {
    this.annotationSetName = annotationSetName;
  }

  public String getAnnotationName() {
    return annotationName;
  }

  @RunTime
  @CreoleParameter
  public void setAnnotationName(String annotationName) {
    this.annotationName = annotationName;
  }
} // class AnnotationDumpPR
