package org.opensextant.regex;

import java.util.regex.MatchResult;

public interface Normalizer {
  public abstract void normalize(RegexAnnotation anno, RegexRule r, MatchResult matchResult);
}