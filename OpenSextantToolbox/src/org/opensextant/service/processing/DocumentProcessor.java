package org.opensextant.service.processing;

import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;

public class DocumentProcessor {

	private CorpusController controller;
	private Corpus corpus;

	public DocumentProcessor(CorpusController cont) {
		this.controller = cont;
	}

	public synchronized void cleanup() {
		Factory.deleteResource(controller);
		if (corpus == null) {
			Factory.deleteResource(corpus);
		}
	}

	public void process(Document doc) {

		if (corpus == null) {

			try {
				corpus = Factory.newCorpus("DP Corpus");
			} catch (ResourceInstantiationException e) {
				e.printStackTrace();
			}
		}

		try {
			corpus.add(doc);
			controller.setCorpus(corpus);

			try {
				controller.execute();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		} finally {
			controller.setCorpus(null);
			corpus.clear();
		}

	}

}
