package org.opensextant.tagger.regex;

import org.opensextant.tagger.Match;

public class RegexAnnotation extends Match {

	private String rule = "";
	private boolean valid = true;

	public RegexAnnotation(String type, String text, int start, int end) {
		this.start = start;
		this.end = end;
		this.type = type;
		this.matchText = text;
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

	public boolean interactsWith(RegexAnnotation other) {

		long s1 = this.start;
		long e1 = this.end;
		long s2 = other.start;
		long e2 = other.end;

		return (s1 >= s2 || e1 >= s2) && (s2 >= s1 || e2 >= s1);
	}

	@Override
	public String toString() {
		return String.format("%s (%s %s %s %s)", this.getMatchText(), this.getType(), this.getStart(), this.getEnd(),
				this.getFeatures());
	}

}