package org.opensextant.regex;

import java.util.regex.MatchResult;

public class NoOpNormalizer implements Normalizer {
	@Override
	public void normalize(RegexAnnotation anno, RegexRule r, MatchResult matchResult) {
		return;
	}
}
