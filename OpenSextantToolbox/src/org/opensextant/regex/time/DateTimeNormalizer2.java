package org.opensextant.regex.time;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opensextant.regex.Normalizer;
import org.opensextant.regex.RegexAnnotation;
import org.opensextant.regex.RegexRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateTimeNormalizer2 implements Normalizer {

  // Enum representing YEAR, MON, WEEK, DAY, HR, MIN
  public enum TimeResolution {
    NONE, YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND
  };
  
  private static List<String> jodaNames = new ArrayList<String>();
  static {
    jodaNames.add("G");
    jodaNames.add("YYYY");
    jodaNames.add("YY");
    jodaNames.add("E");
    jodaNames.add("M");
    jodaNames.add("MM");
    jodaNames.add("MMM");
    jodaNames.add("MMMM");
    jodaNames.add("d");
    jodaNames.add("dd");
    jodaNames.add("a");
    jodaNames.add("K");
    jodaNames.add("h");
    jodaNames.add("H");
    jodaNames.add("k");
    jodaNames.add("m");
    jodaNames.add("mm");
    jodaNames.add("s");
    jodaNames.add("ss");
    jodaNames.add("SS");
    jodaNames.add("z");
  }

  // Log object
  private static Logger log = LoggerFactory.getLogger(DateTimeNormalizer2.class);

  @Override
  public void normalize(RegexAnnotation anno, RegexRule r, MatchResult matchResult) {
    Map<String, Object> normalizedResults = anno.getFeatures();;
    Map<String, String> elementsFound = new HashMap<String, String>();
    int numGroups = matchResult.groupCount();
    for (int i = 0; i < numGroups + 1; i++) {
      // int s = matchResult.start(i);
      // int e = matchResult.end(i);
      String elemenValue = matchResult.group(i);
      String elemName = r.getElementMap().get(i);
      elementsFound.put(elemName, elemenValue);
    }
    // create a reduced match and equivalent format string from each Joda element found
    StringBuffer reducedMatch = new StringBuffer();
    StringBuffer jodaPattern = new StringBuffer();
    for (String elem : elementsFound.keySet()) {
      if (jodaNames.contains(elem)) {
        reducedMatch.append(elementsFound.get(elem) + " ");
        jodaPattern.append(elem + " ");
      }
    }

    // create a formatter from the format string created above
    DateTimeFormatter fmt = null;
    try {
      fmt = DateTimeFormat.forPattern(jodaPattern.toString());
    } catch (Exception e) {
      log.debug("Could not use format " + jodaPattern.toString());
      return;
    }

    // parese the reduced match using the equivalent formatter
    DateTime dt = fmt.parseDateTime(reducedMatch.toString());
    // create a JDK Date to return
    Date jdkDate = dt.toDate();

    normalizedResults.put("hierarchy", "Time.date");
    normalizedResults.put("isEntity", true);
    normalizedResults.put("date", jdkDate);
    return;

  }

}
