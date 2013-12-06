package org.opensextant.regex;

import java.util.List;
import java.util.Set;

public class NoOpPostProcessor implements PostProcessor {

  @Override
  public void postProcess(List<RegexAnnotation> annos, Set<String> types) {
    return;
  }

 

}
