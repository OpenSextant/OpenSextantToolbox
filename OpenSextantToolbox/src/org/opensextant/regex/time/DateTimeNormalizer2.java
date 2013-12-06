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
    NONE, ESTIMATED, ERA, CENTURY, DECADE, YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, FRACTIONAL_SECOND
  };

  // map containing all of the Joda defined formatting elements and their corresponding TimeResolutions
  // NOTE: any element names appearing in a regex other than these will be ignored for normalization purposes
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
    jodaNames.put("hh", TimeResolution.HOUR);
    jodaNames.put("H", TimeResolution.HOUR);
    jodaNames.put("HH", TimeResolution.HOUR);
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

    if (anno.getType().equalsIgnoreCase("Date")) {
      normalizeDate(anno, r, matchResult);
    }

    if (anno.getType().equalsIgnoreCase("Time")) {
      normalizeTime(anno, r, matchResult);
    }

    if (anno.getType().equalsIgnoreCase("DayOfTheMonth")) {
      normalizeDay(anno, r, matchResult);
    }

  }

  public void normalizeDate(RegexAnnotation anno, RegexRule r, MatchResult matchResult) {

    Map<String, Object> normalizedResults = anno.getFeatures();
    Map<String, String> elementsFound = new HashMap<String, String>();
    int numGroups = matchResult.groupCount();
    for (int i = 0; i < numGroups + 1; i++) {
      // int s = matchResult.start(i);
      // int e = matchResult.end(i);
      String elemenValue = matchResult.group(i);
      String elemName = r.getElementMap().get(i);
      elementsFound.put(elemName, elemenValue);
      if (log.isDebugEnabled()) {
        normalizedResults.put(elemName, elemenValue);
      }
    }

    normalizedResults.put("ruleFamily", r.getRuleFamily());
    normalizedResults.put("ruleName", r.getRuleName());

    // create a reduced match and equivalent format string from each Joda element found
    // the reduced match will contain only values relevant to normalization
    StringBuffer reducedMatch = new StringBuffer();
    StringBuffer jodaPattern = new StringBuffer();
    // look for the most precise element in pattern
    TimeResolution mostPrec = TimeResolution.NONE;
    boolean hasYear = false;
    for (String elem : elementsFound.keySet()) {
      String elemValue = elementsFound.get(elem);

      // clean up some specific conditions
      elemValue = cleanValues(elem, elemValue);

      // see if elem is a joda named elem
      if (jodaNames.keySet().contains(elem)) {
        // add elem value to reduced match
        reducedMatch.append(elemValue + " ");
        // add elem to pattern string
        jodaPattern.append(elem + " ");
        // get the resolution for this element
        TimeResolution tr = jodaNames.get(elem);

        // see if we have a year resolution
        if (tr.equals(TimeResolution.YEAR)) {
          hasYear = true;
        }

        // is this the most precise resolution seen?
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
      log.warn("Could not use format " + jodaPattern.toString() + " derived from " + anno.getMatchText()
          + " setting annotation as invalid:" + e.getMessage());
      anno.setValid(false);
      return;
    }

    // check if year value missing
    if (!hasYear) {
      int estYear = getEstimatedYear();
      fmt = fmt.withDefaultYear(estYear);
      log.debug("No year in pattern " + jodaPattern + " using:" + estYear);
      // set time resolution to ESTIMATED to indicate assumption made
      mostPrec = TimeResolution.ESTIMATED;
    }

    // parse the reduced match using the derived formatter
    DateTime dt = null;
    try {
      dt = fmt.parseDateTime(reducedMatch.toString());
      log.debug("Parsing ->" + anno.getMatchText() + "<- reduced to ->" + reducedMatch + "<- as format ->"
          + jodaPattern.toString() + "<- got " + dt.toString());
    } catch (Exception e) {
      log.warn("Cannot normalize " + anno.getMatchText() + " using " + r.toString() + " error was:" + e.getMessage());
      anno.setValid(false);
      return;
    }

    // adjust precision for some specific cases

    // look for phrases like "the 1990's" or "the 1800s"
    if (r.getRuleFamily().equalsIgnoreCase("YEAR")) {
      // get phrase, remove apostrophes and tics
      String phrase = anno.getMatchText().trim().replaceAll("['`]", "");

      if (phrase.endsWith("0s")) {
        mostPrec = TimeResolution.DECADE;
      }
      if (phrase.endsWith("00s")) {
        mostPrec = TimeResolution.CENTURY;
      }

    }

    // create a JDK Date to return
    Date jdkDate = dt.toDate();

    normalizedResults.put("date", jdkDate);
    normalizedResults.put("precision", mostPrec);

    return;
  }

  private void normalizeTime(RegexAnnotation anno, RegexRule r, MatchResult matchResult) {
    Map<String, Object> normalizedResults = anno.getFeatures();
    Map<String, String> elementsFound = new HashMap<String, String>();
    int numGroups = matchResult.groupCount();
    for (int i = 0; i < numGroups + 1; i++) {
      // int s = matchResult.start(i);
      // int e = matchResult.end(i);
      String elemenValue = matchResult.group(i);
      String elemName = r.getElementMap().get(i);
      elementsFound.put(elemName, elemenValue);
      if (log.isDebugEnabled()) {
        normalizedResults.put(elemName, elemenValue);
      }
    }

    normalizedResults.put("ruleFamily", r.getRuleFamily());
    normalizedResults.put("ruleName", r.getRuleName());
    normalizedResults.put("precision", TimeResolution.NONE);
    return;
  }

  private void normalizeDay(RegexAnnotation anno, RegexRule r, MatchResult matchResult) {
    Map<String, Object> normalizedResults = anno.getFeatures();
    Map<String, String> elementsFound = new HashMap<String, String>();
    int numGroups = matchResult.groupCount();
    for (int i = 0; i < numGroups + 1; i++) {
      // int s = matchResult.start(i);
      // int e = matchResult.end(i);
      String elemenValue = matchResult.group(i);
      String elemName = r.getElementMap().get(i);
      elementsFound.put(elemName, elemenValue);
      if (log.isDebugEnabled()) {
        normalizedResults.put(elemName, elemenValue);
      }
    }

    normalizedResults.put("ruleFamily", r.getRuleFamily());
    normalizedResults.put("ruleName", r.getRuleName());
    normalizedResults.put("precision", TimeResolution.NONE);
    return;
  }

  // TODO decide on value for missing year
  // if the pattern does not include some form of "YEAR", use
  // current year?, Joda default year (2000),? year 0000?
  // get a year value when none has been found
  private int getEstimatedYear() {
    DateTime now = new DateTime();
    return now.getYear();
  }

  // some hackery to convert some values to those that Joda recognizes
  private String cleanValues(String elemName, String elemValue) {

    // strip trailing periods on abbreviated months and add "SEPT" as valid abbrev
    if (elemName.equals("MMM")) {
      elemValue = elemValue.replaceFirst("\\.$", "").replaceFirst("(?i:sept)", "Sep");
    }

    // strip leading apostrophe/tic from abbreviated year
    if (elemName.equals("YY")) {
      elemValue = elemValue.replaceFirst("^['`]", "");
    }

    // strip trailing ordinals from days
    if (elemName.equals("dd")) {
      elemValue = elemValue.replaceFirst("(st|nd|rd|th|ST|ND|RD|TH)$", "");
    }

    // strip periods from abbreviated eras
    if (elemName.equals("G")) {
      elemValue = elemValue.replaceAll("\\.", "").toUpperCase();
    }
    
    // convert Z,ZULU and UTC timezones to GMT
    if (elemName.equals("z")
        && (elemValue.equalsIgnoreCase("Z") || elemValue.equalsIgnoreCase("ZULU") || elemValue.equalsIgnoreCase("UTC"))) {
      elemValue = "GMT";
    }

    return elemValue;
  }

}
