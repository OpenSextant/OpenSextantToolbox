package org.opensextant.regex.time;

import java.util.Date;
import java.util.HashMap;
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

  // Enum representing the specificity of the date time reference
  public enum TimeResolution {
    ESTIMATED,NONE, ERA, CENTURY, YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, FRACTIONAL_SECOND
  };

  private static Map<String, TimeResolution> jodaNames = new HashMap<String, TimeResolution>();
  static {
    jodaNames.put("G", TimeResolution.ERA);
    jodaNames.put("YYYY", TimeResolution.YEAR);
    jodaNames.put("YY", TimeResolution.YEAR);
    jodaNames.put("E", TimeResolution.NONE);
    jodaNames.put("M", TimeResolution.MONTH);
    jodaNames.put("MM", TimeResolution.MONTH);
    jodaNames.put("MMM", TimeResolution.MONTH);
    jodaNames.put("MMMM", TimeResolution.MONTH);
    jodaNames.put("d", TimeResolution.DAY);
    jodaNames.put("dd", TimeResolution.DAY);
    jodaNames.put("a", TimeResolution.NONE);
    jodaNames.put("K", TimeResolution.HOUR);
    jodaNames.put("h", TimeResolution.HOUR);
    jodaNames.put("H", TimeResolution.HOUR);
    jodaNames.put("k", TimeResolution.HOUR);
    jodaNames.put("m", TimeResolution.MINUTE);
    jodaNames.put("mm", TimeResolution.MINUTE);
    jodaNames.put("s", TimeResolution.SECOND);
    jodaNames.put("ss", TimeResolution.SECOND);
    jodaNames.put("SS", TimeResolution.FRACTIONAL_SECOND);
    jodaNames.put("z", TimeResolution.NONE);
  }

  // Log object
  private static Logger log = LoggerFactory.getLogger(DateTimeNormalizer2.class);

  @Override
  public void normalize(RegexAnnotation anno, RegexRule r, MatchResult matchResult) {
    Map<String, Object> normalizedResults = anno.getFeatures();
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
    // look for the most precise element in pattern
    TimeResolution mostPrec = TimeResolution.NONE;
    boolean hasYear = false;
    for (String elem : elementsFound.keySet()) {
      if (jodaNames.keySet().contains(elem)) {
        reducedMatch.append(elementsFound.get(elem) + " ");
        jodaPattern.append(elem + " ");
        TimeResolution tr = jodaNames.get(elem);
        
        if(tr.equals(TimeResolution.YEAR)){
          hasYear = true;
        }
        
        if (tr.compareTo(mostPrec) > 0) {
          mostPrec = jodaNames.get(elem);
        }
      }
    }

    // create a Joda formatter from the format string created above
    DateTimeFormatter fmt = null;

    try {
      fmt = DateTimeFormat.forPattern(jodaPattern.toString());
    } catch (Exception e) {
      log.debug("Could not use format " + jodaPattern.toString());
      anno.setValid(false);
      return;
    }

    // TODO decide on value for missing year
    // if the pattern does not include some form of "YEAR", use
    // current year?, Joda default year (2000),? year 0000?
    // set time resolution to ESTIMATED to indicate assumption made
    if (!hasYear) {
      DateTime now = new DateTime();
      fmt = fmt.withDefaultYear(now.getYear());
      log.debug("No year in pattern " + jodaPattern  + " using:" + now.getYear());
      mostPrec = TimeResolution.ESTIMATED;
    }

    // parse the reduced match using the equivalent formatter
    DateTime dt = fmt.parseDateTime(reducedMatch.toString());
    // create a JDK Date to return
    Date jdkDate = dt.toDate();
    normalizedResults.put("date", jdkDate);
    normalizedResults.put("precision", mostPrec);
    return;

  }

}
