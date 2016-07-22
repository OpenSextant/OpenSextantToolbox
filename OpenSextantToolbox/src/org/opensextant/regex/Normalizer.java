package org.opensextant.regex;

import java.util.regex.MatchResult;

import org.opensextant.placedata.AnnotationOS;

public interface Normalizer {
  void normalize(AnnotationOS annotation, RegexRule r, MatchResult matchResult);
}