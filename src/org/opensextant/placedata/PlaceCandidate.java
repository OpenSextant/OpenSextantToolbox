/*
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
package org.opensextant.placedata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensextant.tagger.Match;

/**
 * A PlaceCandidate represents a portion of a document which has been identified
 * as a possible named geographic location. It is used to collect together the
 * information from the document (the evidence), as well as the possible
 * geographic locations it could represent (the Places ). It also contains the
 * results of the final decision to include:
 * <ul>
 * <li>placeConfidenceScore - Confidence that this is actually a place and not a
 * person, organization, or other type of entity.
 * <li>bestPlace - Of all the places with the same/similar names, which place is
 * it?
 * </ul>
 */
public class PlaceCandidate extends Match implements Serializable {
	private static final long serialVersionUID = 1L;

	public String getPlaceName(){
		return this.getMatchText();
	}

	public void setPlaceName(String name){
		this.setMatchText(name);
	}

	/**
	 * --------------Place/NotPlace stuff ---------------------- which rules
	 * have expressed a Place/NotPlace opinion on this PC.
	 */
	private transient List<String> rules;

	/** The confidence adjustments provided by the Place/NotPlace rules. */
	private transient List<Double> placeConfidences;

	/**
	 * --------------Disambiguation stuff ---------------------- the places
	 * along with their disambiguation scores.
	 */
	private transient Map<Place, Double> scoredPlaces;

	/** Temporary lists to hold the ranked places and scores. */
	private transient List<Place> rankedPlaces;
	private transient List<Double> rankedScores;

	/**
	 * The list of PlaceEvidences accumulated from the document about this PC.
	 */
	private transient List<PlaceEvidence> evidence;

	/** Basic constructor. */
	public PlaceCandidate() {
		scoredPlaces = new HashMap<Place, Double>();
		rankedPlaces = new ArrayList<Place>();
		rankedScores = new ArrayList<Double>();
		evidence = new ArrayList<PlaceEvidence>();
		rules = new ArrayList<String>();
		placeConfidences = new ArrayList<Double>();
	}

	// ---- the getters and setters ---------
	/**
	 * Get the most highly ranked Place, or Null if empty list.
	 */
	public Place getBestPlace() {
		sort();
		List<Place> l = getPlaces();
		if (l.isEmpty()) {
			return null;
		}
		return l.get(0);
	}

	/**
	 * Get the disambiguation score of the most highly ranked Place, or 0.0 if
	 * empty list.
	 */
	public double getBestPlaceScore() {
		sort();
		List<Double> l = getScores();
		if (l.isEmpty()) {
			return 0.0;
		}
		return l.get(0);
	}

	/**
	 * Does our confidence indicate that this is actually a place?
	 */
	public boolean isPlace() {
		return getPlaceConfidenceScore() > 0.0;
	}


	/**
	 * Get a ranked list of places.
	 */
	public List<Place> getPlaces() {
		sort();
		return this.rankedPlaces;
	}

	/**
	 * Get a ranked list of scores.
	 */
	public List<Double> getScores() {
		sort();
		return this.rankedScores;
	}

	/** Add a new place with a default score. */
	public void addPlace(Place place) {
		addPlaceWithScore(place, 0.0);
	}

	/** Add a new place with a specific score. */
	public void addPlaceWithScore(Place place, double score) {
		this.scoredPlaces.put(place, score);
	}

	/** Increment the score of an existing place. */
	public void incrementPlaceScore(Place place, double score) {
		Double currentScore = this.scoredPlaces.get(place);
		if (currentScore != null) {
			this.scoredPlaces.put(place, currentScore + score);
		}
	}

	/** Set the score of an existing place. */
	public void setPlaceScore(Place place, double score) {
		if (!this.scoredPlaces.containsKey(place)) {
			return;
		}
		this.scoredPlaces.put(place, score);
	}

	public List<String> getRules() {
		return rules;
	}

	public List<Double> getConfidences() {
		return placeConfidences;
	}

	/** Check if at least one of the Places has the given country code. */
	public boolean possibleCountry(String cc) {
		for (Place p : rankedPlaces) {
			if (p.getCountryCode() != null && p.getCountryCode().equalsIgnoreCase(cc)) {
				return true;
			}
		}
		return false;
	}

	/** Check if at least one of the Places has the given admin code. */
	public boolean possibleAdmin(String adm, String cc) {
		// check the non-null admins first
		for (Place p : rankedPlaces) {
			if (p.getAdmin1() != null && p.getAdmin1().equalsIgnoreCase(adm)) {
				return true;
			}
		}
		// some adm1codes are null, a null admin of the correct country could be
		// possible match
		for (Place p : rankedPlaces) {
			if (p.getAdmin1() == null && p.getCountryCode().equalsIgnoreCase(cc)) {
				return true;
			}
		}
		return false;
	}

	public void addRuleAndConfidence(String rule, double conf) {
		rules.add(rule);
		placeConfidences.add(conf);
	}

	/**
	 * Get the PlaceConfidence score. This is the confidence that this
	 * PlaceCandidate represents a named place and not a person,organization or
	 * other entity.
	 */
	public double getPlaceConfidenceScore() {
		if (placeConfidences.isEmpty()) {
			return 0.0;
		}
		// average of placeConfidences
		double total = 0.0;
		for (double tmpScore : placeConfidences) {
			total = total + tmpScore;
		}
		double tmp = total / placeConfidences.size();
		// ensure the final score is within +-1.0
		if (tmp > 1.0) {
			tmp = 1.0;
		}
		if (tmp < -1.0) {
			tmp = -1.0;
		}
		return tmp;
	}

	/**
	 * Set the PlaceConfidence score to a specific value. NOTE: This method is
	 * only intended to be used in calibration/testing, it would not normally be
	 * used in production.Note that it removes any existing rules and
	 * confidences.
	 */
	public void setPlaceConfidenceScore(double score) {
		placeConfidences.clear();
		rules.clear();
		if (Math.abs(score) > 0.0) { // don't add a 0.0 strength rule
			addRuleAndConfidence("Calibrate", score);
		}
	}

	public void addEvidence(PlaceEvidence evidence) {
		this.evidence.add(evidence);
	}

	/** Some convenience methods to add evidence. */
	public void addEvidence(String rule, double weight, String cc, String adm1, String fclass, String fcode,
			Geocoord geo) {
		PlaceEvidence ev = new PlaceEvidence();
		ev.setRule(rule);
		ev.setWeight(weight);
		if (cc != null) {
			ev.setCountryCode(cc);
		}
		if (adm1 != null) {
			ev.setAdmin1(adm1);
		}
		if (fclass != null) {
			ev.setFeatureClass(fclass);
		}
		if (fcode != null) {
			ev.setFeatureCode(fcode);
		}
		if (geo != null) {
			ev.setGeocoord(geo);
		}
		this.evidence.add(ev);
	}

	public void addCountryEvidence(String rule, Double weight, String cc) {
		PlaceEvidence ev = new PlaceEvidence();
		ev.setRule(rule);
		ev.setWeight(weight);
		ev.setCountryCode(cc);
		this.evidence.add(ev);
	}

	public void addAdmin1Evidence(String rule, double weight, String adm1, String cc) {
		PlaceEvidence ev = new PlaceEvidence();
		ev.setRule(rule);
		ev.setWeight(weight);
		ev.setAdmin1(adm1);
		ev.setCountryCode(cc);
		this.evidence.add(ev);
	}

	public void addFeatureClassEvidence(String rule, double weight, String fclass) {
		PlaceEvidence ev = new PlaceEvidence();
		ev.setRule(rule);
		ev.setWeight(weight);
		ev.setFeatureClass(fclass);
		this.evidence.add(ev);
	}

	public void addFeatureCodeEvidence(String rule, double weight, String fcode) {
		PlaceEvidence ev = new PlaceEvidence();
		ev.setRule(rule);
		ev.setWeight(weight);
		ev.setFeatureCode(fcode);
		this.evidence.add(ev);
	}

	public void addGeocoordEvidence(String rule, double weight, Geocoord coord) {
		PlaceEvidence ev = new PlaceEvidence();
		ev.setRule(rule);
		ev.setWeight(weight);
		ev.setGeocoord(coord);
		this.evidence.add(ev);
	}

	public List<PlaceEvidence> getEvidence() {
		return this.evidence;
	}

	/** Convenience method for determining the state of a PlaceCandidate. */
	public boolean hasPlaces() {
		return !this.scoredPlaces.isEmpty();
	}

	private void sort() {
		this.rankedPlaces.clear();
		this.rankedScores.clear();
		List<ScoredPlace> tmp = new ArrayList<ScoredPlace>();
		for (Place pl : this.scoredPlaces.keySet()) {
			tmp.add(new ScoredPlace(pl, scoredPlaces.get(pl)));
		}
		Collections.sort(tmp);
		for (ScoredPlace spl : tmp) {
			this.rankedPlaces.add(spl.getPlace());
			this.rankedScores.add(spl.getScore());
		}
	}

	/** An overide of toString to get a meaningful representation of this PC. */
	@Override
	public String toString() {
		String tmp = this.getMatchText() + "(" + getPlaceConfidenceScore() + "/" + this.scoredPlaces.size() + ")" + "\n";
		tmp = tmp + "Rules=" + this.rules + "\n";
		tmp = tmp + "Evidence=" + this.evidence + "\n";
		sort();
		tmp = tmp + "Places=";
		for (int i = 0; i < this.rankedPlaces.size(); i++) {
			tmp = tmp + this.rankedPlaces.get(i) + "=" + this.rankedScores.get(i) + "\n";
		}
		return tmp;
	}
}
