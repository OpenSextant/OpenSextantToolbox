package org.opensextant.regex;

import java.util.List;
import java.util.Set;

import org.opensextant.placedata.AnnotationOS;

public class NoOpPostProcessor implements PostProcessor {

  @Override
  public void postProcess(List<AnnotationOS> annotations, Set<String> types) {
    return;
  }

}
