package org.opensextant.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.opensextant.vocab.Vocab;
import org.opensextant.vocab.VocabMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabMatcher {

  private SolrServer solrServer;
  private ModifiableSolrParams matchParams;

  private SolrTaggerRequest tagRequest;
  private Map<Integer, Vocab> vocabIDMap = new HashMap<Integer, Vocab>(100);

  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(VocabMatcher.class);

  protected VocabMatcher(SolrServer svr, ModifiableSolrParams prms) {
    this.solrServer = svr;
    matchParams = new ModifiableSolrParams(prms);
  }

  public List<VocabMatch> matchText(String buffer, String docName) {

    List<VocabMatch> matches = new ArrayList<VocabMatch>();
    // Setup request to tag
    tagRequest = new SolrTaggerRequest(matchParams, SolrRequest.METHOD.POST);
    tagRequest.setInput(buffer);

    QueryResponse response = null;

    try {
      response = tagRequest.process(solrServer);
    } catch (SolrServerException e) {
      LOGGER.error("Got exception when attempting to match " + docName, e);
      return matches;
    }

    // Process Solr Response
    SolrDocumentList docList = response.getResults();

    // TODO convert this section to use a StreamingResponseCallback

    // convert each solrdoc (a match) to a VocabMatch and add to id map
    for (SolrDocument solrDoc : docList) {
      Integer id = (Integer) solrDoc.getFirstValue("id");
      Vocab match = MatcherFactory.createVocab(solrDoc);
      vocabIDMap.put(id, match);
    }

    @SuppressWarnings("unchecked")
    List<NamedList<?>> tags = (List<NamedList<?>>) response.getResponse().get("tags");

    VocabMatch mt = null;
    int x1 = -1, x2 = -1;

    String matchText = null;

    for (NamedList<?> tag : tags) {
      // get the start, end and list of matching place IDs
      x1 = (Integer) tag.get("startOffset");
      x2 = (Integer) tag.get("endOffset");
      @SuppressWarnings("unchecked")
      List<Integer> vocabIDList = (List<Integer>) tag.get("ids");

      // create and populate the VocabMatch
      mt = new VocabMatch();
      mt.setStart(x1);
      mt.setEnd(x2);
      matchText = buffer.substring(x1, x2);
      mt.setTextMatch(matchText);
      for (Integer vocabID : vocabIDList) {
        Vocab v = vocabIDMap.get(vocabID);
        mt.addVocab(v);
      }
      matches.add(mt);
    }

    return matches;

  }

  public void cleanup() {
    MatcherFactory.shutdown(this);
  }
}
