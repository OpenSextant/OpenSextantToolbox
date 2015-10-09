package org.opensextant.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.opensextant.placedata.Geocoord;
import org.opensextant.placedata.Place;
import org.opensextant.placedata.ScoredPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlacenameSearcher {

  private SolrServer solrServer;
  private ModifiableSolrParams baseSearchParams = new ModifiableSolrParams();

  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(PlacenameSearcher.class);

  protected PlacenameSearcher(SolrServer svr, ModifiableSolrParams prms) {
    solrServer = svr;
    baseSearchParams = new ModifiableSolrParams(prms);
  }

  private List<Place> search(ModifiableSolrParams prms) {

    List<Place> places = new ArrayList<Place>();

    QueryResponse response = null;
    try {
      response = solrServer.query(prms);
    } catch (SolrServerException e) {
      LOGGER.error("Got exception when processing query.", e);
      return places;
    }

    if (response != null) {
      SolrDocumentList docList = response.getResults();
      for (SolrDocument d : docList) {
        Place p = MatcherFactory.createPlace(d);
        places.add(p);
      }
    }
    return places;

  }

  public SolrDocumentList dumpDocs(String q) {

    ModifiableSolrParams srchParams = new ModifiableSolrParams(baseSearchParams);
    srchParams.set("q", q);
    QueryResponse response = null;
    try {
      response = solrServer.query(srchParams);
    } catch (SolrServerException e) {
      LOGGER.error("Got exception when processing query.", e);
      return null;
    }

    if (response != null) {
      return response.getResults();
    }

    return null;

  }

  public List<Place> searchByQueryString(String query) {
    ModifiableSolrParams srchParams = new ModifiableSolrParams(baseSearchParams);
    srchParams.set("q", query);
    return search(srchParams);
  }

  public List<Place> searchByPlaceName(String placeName, boolean fuzzy) {
    ModifiableSolrParams srchParams = new ModifiableSolrParams(baseSearchParams);
    String query = "name:";
    if (fuzzy) {
      query = query + placeName + "~0.80";
    } else {
      query = query + "\"" + placeName + "\"";
    }

    srchParams.set("defType", "edismax");

    srchParams.set("q", query);
    return search(srchParams);
  }

  /** Distance in kilometers. */
  public List<ScoredPlace> searchByCircle(Geocoord center, double distance) {
    return searchByCircle(center.getLatitude(), center.getLongitude(), distance);
  }

  /** Distance in kilometers. */
  public List<ScoredPlace> searchByCircle(double lat, double lon, double distance) {
    ModifiableSolrParams srchParams = new ModifiableSolrParams(baseSearchParams);
    String query = "{!geofilt pt=" + lat + "," + lon + " sfield=geo" + " d=" + distance + "}";

    srchParams.set("q", query);

    List<ScoredPlace> places = new ArrayList<ScoredPlace>();

    QueryResponse response = null;
    try {
      response = solrServer.query(srchParams);
    } catch (SolrServerException e) {
      LOGGER.error("Got exception when processing query.", e);
      return places;
    }

    if (response != null) {
      SolrDocumentList docList = response.getResults();
      for (SolrDocument d : docList) {
        Place p = MatcherFactory.createPlace(d);
        double dist = p.getGeocoord().distance(lat, lon);
        places.add(new ScoredPlace(p, dist));
      }
    }

    // sort by distance (ascending)
    Collections.sort(places);
    Collections.reverse(places);

    return places;

  }

  /** TODO add search variants for exact/inexact name, constraints (country,feature type), geo radius ... */

  public void cleanup() {
    MatcherFactory.shutdown(this);
  }
}
