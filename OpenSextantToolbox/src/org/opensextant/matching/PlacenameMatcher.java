package org.opensextant.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.opensextant.placedata.Place;
import org.opensextant.placedata.PlaceCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlacenameMatcher {

  private SolrServer solrServer;
  private ModifiableSolrParams matchParams;

  private static final String APRIORI_NAME_RULE = "AprioriNameBias";
  private SolrTaggerRequest tagRequest;
  private Map<Integer, Place> placeIDMap = new HashMap<Integer, Place>(100);
  private boolean tagAbbrev;

  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(PlacenameMatcher.class);

  protected PlacenameMatcher(SolrServer svr, ModifiableSolrParams prms) {
    this.solrServer = svr;
    matchParams = new ModifiableSolrParams(prms);
  }

  public void tagAbbreviations(boolean b) {
    tagAbbrev = b;
  }

  public List<PlaceCandidate> matchText(String buffer, String docName) {

    List<PlaceCandidate> candidates = new ArrayList<PlaceCandidate>();
    // Setup request to tag
    tagRequest = new SolrTaggerRequest(matchParams, SolrRequest.METHOD.POST);
    tagRequest.setInput(buffer);

    QueryResponse response = null;

    try {
      response = tagRequest.process(solrServer);
    } catch (SolrServerException e) {
      LOGGER.error("Got exception when attempting to match " + docName, e);
      return candidates;
    }

    // Process Solr Response
    SolrDocumentList docList = response.getResults();

    // TODO convert this section to use a StreamingResponseCallback

    // clear out the place id map
    placeIDMap.clear();

    // populate the place id map from the solr documents
    for (SolrDocument solrDoc : docList) {
      Integer id = (Integer) solrDoc.getFirstValue("id");
      Place place = MatcherFactory.createPlace(solrDoc);
      placeIDMap.put(id, place);
    }

    @SuppressWarnings("unchecked")
    List<NamedList<?>> tags = (List<NamedList<?>>) response.getResponse().get("tags");

    PlaceCandidate pc = null;
    int x1 = -1, x2 = -1;
    Set<String> seenPlaces = new HashSet<String>();
    double nameBias = 0.0;
    String matchText = null;

    for (NamedList<?> tag : tags) {
      // clear out seen places set
      seenPlaces.clear();

      // get the start, end and list of matching place IDs
      x1 = (Integer) tag.get("startOffset");
      x2 = (Integer) tag.get("endOffset");
      @SuppressWarnings("unchecked")
      List<Integer> placeIDList = (List<Integer>) tag.get("ids");

      // create and populate the PlaceCandidate
      pc = new PlaceCandidate();
      pc.setStart(x1);
      pc.setEnd(x2);
      matchText = buffer.substring(x1, x2);
      pc.setPlaceName(matchText);
      nameBias = 0.0;

      boolean isValid = true;
      boolean isLower = StringUtils.isAllLowerCase(matchText);

      for (Integer placeID : placeIDList) {
        // get the Place that corresponds to this ID
        Place place = placeIDMap.get(placeID);

        // don't tag if place name is an abbrev and matchtext is all lower case
        if (!tagAbbrev && place.isAbbreviation() && isLower) {
          isValid = false;
          LOGGER.debug("Not tagging abbreviation:" + matchText);
          break;
        }

        // don't add places already on candidate
        if (!seenPlaces.contains(place.getPlaceID())) {
          pc.addPlaceWithScore(place, place.getIdBias());
          seenPlaces.add(place.getPlaceID());
          // get max name bias
          double nBias = place.getNameBias();
          if (nBias > nameBias) {
            nameBias = nBias;
          }
        }
      }// end placeID loop

      if (!isValid || !pc.hasPlaces()) {
        continue;
      }

      // if the max name bias seen >0; add apriori evidence
      if (nameBias > 0.0) {
        pc.addRuleAndConfidence(APRIORI_NAME_RULE, nameBias);
      }
      candidates.add(pc);
    }

    // clear out the place id map
    placeIDMap.clear();
    return candidates;

  }

  public void cleanup() {
    MatcherFactory.shutdown(this);
  }
}
