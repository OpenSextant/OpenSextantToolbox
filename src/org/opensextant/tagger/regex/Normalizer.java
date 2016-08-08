package org.opensextant.tagger.regex;

import java.util.regex.MatchResult;

import org.opensextant.tagger.Match;

public interface Normalizer {
	void normalize(Match anno, RegexRule r, MatchResult matchResult);
}