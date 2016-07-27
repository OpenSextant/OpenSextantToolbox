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

  /** Enum representing the specificity of the date time reference. */
  public enum TimeResolution {
    NONE, ESTIMATED, ERA, CENTURY, DECADE, YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, FRACTIONAL_SECOND
  }

  /**
   * Map containing all of the Joda defined formatting elements and their corresponding TimeResolutions NOTE: any
   * element names appearing in a regex other than these will be ignored for normalization purposes
   */
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

  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(DateTimeNormalizer2.class);

  @Override
  public void normalize(RegexAnnotation anno, RegexRule r, MatchResult matchResult) {

    if ("Date".equalsIgnoreCase(anno.getType())) {
      normalizeDate(anno, r, matchResult);
    }

    if ("Time".equalsIgnoreCase(anno.getType())) {
      normalizeTime(anno, r, matchResult);
    }

    if ("DayOfTheMonth".equalsIgnoreCase(anno.getType())) {
      normalizeDay(anno, r, matchResult);
    }

  }

  public void normalizeDate(RegexAnnotation anno, RegexRule r, MatchResult matchResult) {

    Map<String, Object> normalizedResults = anno.getFeatures();
    Map<String, String> elementsFound = new HashMap<String, String>();
    int numGroups = matchResult.groupCount();
    for (int i = 0; i < numGroups + 1; i++) {
      String elemenValue = matchResult.group(i);
      String elemName = r.getElementMap().get(i);
      elementsFound.put(elemName, elemenValue);
      if (LOGGER.isDebugEnabled()) {
        normalizedResults.put(elemName, elemenValue);
      }
    }

    normalizedResults.put("ruleFamily", r.getRuleFamily());
    normalizedResults.put("ruleName", r.getRuleName());

    // create a reduced match and equivalent format string from each Joda element found
    // the reduced match will contain only values relevant to normalization
    StringBuilder reducedMatch = new StringBuilder();
    StringBuilder jodaPattern = new StringBuilder();
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
        reducedMatch.append(elemValue).append(" ");
        // add elem to pattern string
        jodaPattern.append(elem).append(" ");
        // get the resolution for this element
        TimeResolution tr = jodaNames.get(elem);

        // see if we have a year resolution
        if (TimeResolution.YEAR.equals(tr)) {
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
      LOGGER.warn("Could not use format " + jodaPattern + " derived from " + anno.getMatchText()
          + " setting annotation as invalid", e);
      anno.setValid(false);
      return;
    }

    // check if year value missing
    if (!hasYear) {
      int estYear = getEstimatedYear();
      fmt = fmt.withDefaultYear(estYear);
      LOGGER.debug("No year in pattern " + jodaPattern + " using:" + estYear);
      // set time resolution to ESTIMATED to indicate assumption made
      mostPrec = TimeResolution.ESTIMATED;
    }

    // parse the reduced match using the derived formatter
    DateTime dt = null;
    try {
      dt = fmt.parseDateTime(reducedMatch.toString());
      LOGGER.debug("Parsing ->" + anno.getMatchText() + "<- reduced to ->" + reducedMatch + "<- as format ->"
          + jodaPattern + "<- got " + dt);
    } catch (Exception e) {
      LOGGER.warn("Cannot normalize " + anno.getMatchText() + " using " + r, e);
      anno.setValid(false);
      return;
    }

    // adjust precision for some specific cases

    // look for phrases like "the 1990's" or "the 1800s"
    if ("YEAR".equalsIgnoreCase(r.getRuleFamily())) {
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
      String elemenValue = matchResult.group(i);
      String elemName = r.getElementMap().get(i);
      elementsFound.put(elemName, elemenValue);
      if (LOGGER.isDebugEnabled()) {
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
      String elemenValue = matchResult.group(i);
      String elemName = r.getElementMap().get(i);
      elementsFound.put(elemName, elemenValue);
      if (LOGGER.isDebugEnabled()) {
        normalizedResults.put(elemName, elemenValue);
      }
    }

    normalizedResults.put("ruleFamily", r.getRuleFamily());
    normalizedResults.put("ruleName", r.getRuleName());
    normalizedResults.put("precision", TimeResolution.NONE);
    return;
  }

  /**
   * TODO decide on value for missing year if the pattern does not include some form of "YEAR", use current year?, Joda
   * default year (2000),? year 0000? get a year value when none has been found
   */
  private int getEstimatedYear() {
    DateTime now = new DateTime();
    return now.getYear();
  }

  /** Some hackery to convert some values to those that Joda recognizes. */
  private String cleanValues(String elemName, String elemValue) {

    String cleanValue = elemValue;
    // strip trailing periods on abbreviated months and add "SEPT" as valid abbrev
    if ("MMM".equals(elemName)) {
      cleanValue = cleanValue.replaceFirst("\\.$", "").replaceFirst("(?i:sept)", "Sep");
    }

    // strip leading apostrophe/tic from abbreviated year
    if ("YY".equals(elemName)) {
      cleanValue = cleanValue.replaceFirst("^['`]", "");
    }

    // strip trailing ordinals from days
    if ("dd".equals(elemName)) {
      cleanValue = cleanValue.replaceFirst("(st|nd|rd|th|ST|ND|RD|TH)$", "");
    }

    // strip periods from abbreviated eras
    if ("G".equals(elemName)) {
      cleanValue = cleanValue.replaceAll("\\.", "").toUpperCase();
    }

    // convert Z,ZULU and UTC timezones to GMT
    if ("z".equals(elemName)
        && ("Z".equalsIgnoreCase(cleanValue) || "ZULU".equalsIgnoreCase(cleanValue) || "UTC"
            .equalsIgnoreCase(cleanValue))) {
      cleanValue = "GMT";
    }

    // strip periods from am/pm eras
    if ("a".equals(elemName)) {
      cleanValue = cleanValue.replaceAll("\\.", "").toUpperCase();
    }

    return cleanValue;
  }

}
