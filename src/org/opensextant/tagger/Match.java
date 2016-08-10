package org.opensextant.tagger;

import java.util.HashMap;
import java.util.Map;

public class Match {

	protected long start;
	protected long end;
	protected String type = "";
	protected String matchText = "";
	private String rule = "";
	private boolean valid = true;

	protected Map<String, Object> features = new HashMap<String, Object>();

	public Match(String type, String text, int start, int end) {
		this.start = start;
		this.end = end;
		this.type = type;
		this.matchText = text;
	}

	public Match() {
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMatchText() {
		return matchText;
	}

	public void setMatchText(String matchText) {
		this.matchText = matchText;
	}

	public Map<String, Object> getFeatures() {
		return features;
	}

	public void setFeatures(Map<String, Object> features) {
		this.features = features;
	}

	public void addFeature(String featureName, Object featureValue) {
		this.features.put(featureName, featureValue);
	}

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean interactsWith(Match other) {

		long s1 = this.start;
		long e1 = this.end;
		long s2 = other.start;
		long e2 = other.end;

		return (s1 >= s2 || e1 >= s2) && (s2 >= s1 || e2 >= s1);
	}

	public String toString() {

		return this.matchText + " (" + this.type + ")";

	}

}
