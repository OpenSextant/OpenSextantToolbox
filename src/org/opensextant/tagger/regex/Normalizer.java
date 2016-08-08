package org.opensextant.tagger.regex;

import java.util.regex.MatchResult;

public interface Normalizer {
	void normalize(RegexAnnotation anno, RegexRule r, MatchResult matchResult);
}