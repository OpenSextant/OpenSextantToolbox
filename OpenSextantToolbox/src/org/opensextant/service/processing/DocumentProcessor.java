package org.opensextant.service.processing;

import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentProcessor {

  private CorpusController controller;
  private Corpus corpus;
  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentProcessor.class);

  public DocumentProcessor(CorpusController cont) {
    this.controller = cont;
  }

  public synchronized void cleanup() {
    Factory.deleteResource(controller);
    if (corpus != null) {
      Factory.deleteResource(corpus);
    }
  }

  public void process(Document doc) {

    if (corpus == null) {

      try {
        corpus = Factory.newCorpus("DP Corpus");
      } catch (ResourceInstantiationException e) {
        LOGGER.error("Couldnt create new corpus", e);
      }
    }

    try {
      corpus.add(doc);
      controller.setCorpus(corpus);

      try {
        controller.execute();
      } catch (ExecutionException e) {
        LOGGER.error("Couldnt execute document processing", e);
      }
    } finally {
      controller.setCorpus(null);
      corpus.clear();
    }

  }

}
