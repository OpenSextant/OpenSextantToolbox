package org.opensextant.regex;

import java.util.Map;
import java.util.regex.MatchResult;

public class DebugNormalizer implements Normalizer {

  // Log object
  // private static Logger log = LoggerFactory.getLogger(DebugNormalizer.class);
  @Override
  public void normalize(RegexAnnotation anno, RegexRule r, MatchResult matchResult) {

    Map<String, Object> annoFeatures = anno.getFeatures();

    int numGroups = matchResult.groupCount();

    for (int i = 0; i < numGroups + 1; i++) {
      // Future: create sub-annotations?
      // int s = matchResult.start(i);
      // int e = matchResult.end(i);

      String elemenValue = matchResult.group(i);
      String elemName = r.getElementMap().get(i);
      annoFeatures.put(elemName, elemenValue);
    }

    anno.getFeatures().put("entityType", r.getEntityType());
    anno.getFeatures().put("ruleFamily", r.getRuleFamily());
    anno.getFeatures().put("ruleName", r.getRuleName());
  }
}
