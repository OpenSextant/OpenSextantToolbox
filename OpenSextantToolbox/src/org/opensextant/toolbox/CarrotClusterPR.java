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

import gate.Annotation;
import gate.AnnotationSet;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.OffsetComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.stc.STCClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.IClusteringAlgorithm;
import org.carrot2.core.LanguageCode;
import org.carrot2.core.ProcessingResult;

/**
 * This GATE ProcessingResource implements a
 */
@SuppressWarnings("serial")
@CreoleResource(name = "OpenSextant Carrot2 Clusterer", comment = "Adds info to document based on the clusters found")
public class CarrotClusterPR extends AbstractLanguageAnalyser implements ProcessingResource {
  // The parameters passed in by the user
  String inputASName; // The name of the input AnnotationSet
  String outputASName; // The name of the output AnnotationSet
  String sentenceAnnoName; // the annotation to examine,usually "Sentence"
  String subAnnotationName;// annotations within sentenceAnnotation to use
  String clusterAlgo; // algorithm/method used to produce the clusters
  transient Controller controller; // the thing that does all the work

  private void initialize() {

    controller = ControllerFactory.createCachingPooling(IClusteringAlgorithm.class);

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
    // If no output Annotation set was given, append to the input AS
    AnnotationSet annotSet = (outputASName == null || "".equals(outputASName)) ? document.getAnnotations() : document
        .getAnnotations(outputASName);
    // get the annotations to cluster
    AnnotationSet sentSet = annotSet.get(sentenceAnnoName);

    // create a set of Carrot documents from the specific annotations
    List<Document> documents = new ArrayList<Document>();
    for (Annotation an : sentSet) {
      String id = an.getId().toString();
      // if sub annotations are specified, concatenate them into pseudo sentence
      if (subAnnotationName != null && !subAnnotationName.isEmpty()) {
        AnnotationSet subAnnoSet = annotSet.get(subAnnotationName, an.getStartNode().getOffset(), an.getEndNode()
            .getOffset());

        if (subAnnoSet != null && !subAnnoSet.isEmpty()) {
          List<Annotation> anList = new ArrayList<Annotation>(subAnnoSet);
          Collections.sort(anList, new OffsetComparator());
          String pseudoSentence = "";
          for (Annotation np : anList) {
            pseudoSentence = pseudoSentence + " ____ " + gate.Utils.cleanStringFor(document, np);
          }
          an.getFeatures().put("stringToSend", pseudoSentence);
          documents.add(new Document(null, pseudoSentence, null, LanguageCode.ENGLISH, id));
        }
      } else {
        String annoText = gate.Utils.cleanStringFor(document, an);
        documents.add(new Document(null, annoText, null, LanguageCode.ENGLISH, id));
      }

    }
    // do the clustering, using specified algorithm
    ProcessingResult result = null;
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("documents", documents);

    if ("stc".equalsIgnoreCase(clusterAlgo)) {
      attributes.put("STCClusteringAlgorithm.scoreWeight", 0.5);
      result = controller.process(attributes, STCClusteringAlgorithm.class);
    } else if ("kmeans".equalsIgnoreCase(clusterAlgo)) {
      result = controller.process(attributes, BisectingKMeansClusteringAlgorithm.class);
    } else {// default
      attributes.put("LingoClusteringAlgorithm.scoreWeight", 0.5);
      result = controller.process(attributes, LingoClusteringAlgorithm.class);
    }

    // attach cluster info to document and to annotations
    List<Cluster> clusters = result.getClusters();

    List<String> labels = new ArrayList<String>();
    for (Cluster cl : clusters) {
      labels.add(cl.getLabel());
      for (Document d : cl.getDocuments()) {
        int id = Integer.parseInt(d.getStringId());
        Annotation a = document.getAnnotations().get(id);
        // add lable to annotation
        a.getFeatures().put("cluster label", cl.getLabel());
      }
    }
    // add list of labels to document
    document.getFeatures().put("clusterLabels", labels);

  } // end execute

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
   * @param outputASName
   */
  @Optional
  @RunTime
  @CreoleParameter
  public void setOutputASName(String outputASName) {
    this.outputASName = outputASName;
  }

  public String getSentenceAnnoName() {
    return sentenceAnnoName;
  }

  @Optional
  @RunTime
  @CreoleParameter(defaultValue = "Sentence")
  public void setSentenceAnnoName(String annoName) {
    this.sentenceAnnoName = annoName;
  }

  public String getSubAnnoName() {
    return subAnnotationName;
  }

  @Optional
  @RunTime
  @CreoleParameter(defaultValue = "NounPhrase")
  public void setSubAnnoName(String annoName) {
    this.subAnnotationName = annoName;
  }

  /**
   * @return
   */
  public String getPhoneticAlgo() {
    return clusterAlgo;
  }

  /**
   * @param phoneticAlgo
   */
  @Optional
  @RunTime
  @CreoleParameter(defaultValue = "Lingo")
  public void setPhoneticAlgo(String phoneticAlgo) {
    this.clusterAlgo = phoneticAlgo;
  }
}
