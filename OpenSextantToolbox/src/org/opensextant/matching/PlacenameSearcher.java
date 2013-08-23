package org.opensextant.matching;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.opensextant.placedata.Place;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlacenameSearcher {

  private SolrServer solrServer;
  private ModifiableSolrParams searchParams = new ModifiableSolrParams();

  // Log object
  private static Logger log = LoggerFactory.getLogger(PlacenameSearcher.class);

  protected PlacenameSearcher(SolrServer svr, ModifiableSolrParams prms) {
    solrServer = svr;
    searchParams = new ModifiableSolrParams(prms);
  }

  public List<Place> search(String placeName) {

    List<Place> places = new ArrayList<Place>();
    searchParams.set("q", placeName);

    QueryResponse response = null;
    try {
      response = solrServer.query(searchParams);
    } catch (SolrServerException e) {
      log.error("Got exception when processing query.", e);
      return places;
    }

    if (response != null) {
      SolrDocumentList docList = response.getResults();
      for (SolrDocument d : docList) {
        Place p = MatcherFactory.createPlace(d);
        // System.out.println(d.getFieldValue("name") + " " +d.getFieldValue("score") );
        places.add(p);
      }
    }
    return places;

  }

  // TODO add search variants for exact/inexact name, constraints (country,feature type), geo radius ...

  public void cleanup() {
    MatcherFactory.shutdown(this);
  }
}
