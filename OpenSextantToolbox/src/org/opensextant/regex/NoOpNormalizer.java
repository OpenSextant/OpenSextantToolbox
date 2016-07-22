package org.opensextant.regex;

import java.util.regex.MatchResult;

import org.opensextant.placedata.AnnotationOS;

public class NoOpNormalizer implements Normalizer {
  @Override
  public void normalize(AnnotationOS annotation, RegexRule r, MatchResult matchResult) {
    return;
  }
}
