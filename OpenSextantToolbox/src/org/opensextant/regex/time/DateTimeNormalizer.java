package org.opensextant.regex.time;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opensextant.regex.Normalizer;
import org.opensextant.regex.RegexAnnotation;
import org.opensextant.regex.RegexRule;

public class DateTimeNormalizer implements Normalizer {

  // Enum representing YEAR, MON, WEEK, DAY, HR, MIN

  /**
   * A simplistic way to capture resolution of the date/time reference.
   */
  public enum TimeResolution {NONE, YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND};

  static final DateTime CAL = DateTime.now(DateTimeZone.UTC);
  static final int YEAR = CAL.getYear();
  static final int MILLENIUM = 2000;
  static final int CURRENT_YY = YEAR - MILLENIUM;;
  static final int FUTURE_YY_THRESHOLD = CURRENT_YY + 2;
  static final int MAXIMUM_YEAR = 2020;

  // Use of year "15" would imply 1915 in this case.
  // Adjust 2-digit year threshold as needed.
  // Java default is 80/20. 2000 - 2032 is the assumed year for "00" through
  // "32"
  // "33" is 1933
  //

  public static int invalidDate = -1;

  static final DateTimeFormatter MONTH_FORMAT = DateTimeFormat.forPattern("MMM").withZoneUTC();

  // Log object
  // private static Logger log = LoggerFactory.getLogger(DateTimeNormalizer.class);

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
    // String textMatch = matchResult.group(0);

    // Parse years.
    int year = normalizeYear(elementsFound);
    if (year == invalidDate) {
      anno.setValid(false);
      return;
    }

    if (year > MAXIMUM_YEAR) {
      // HHMM can look like a year, e.g., 2100h or 2300 PM
      anno.setValid(false);
      return;
    }

    // dt.resolution = DateMatch.TimeResolution.YEAR;
    int month = 0;
    month = normalizeMonth(elementsFound);

    if (month == invalidDate) {
      month = normalizeMonthName(elementsFound);
    }
    if (month == invalidDate) {
      anno.setValid(false);
      return;
    }

    DateTime cal = new DateTime(year, month, 1, 0, 0, DateTimeZone.UTC);

    // dt.resolution = DateMatch.TimeResolution.MONTH;
    int dom = normalizeDay(elementsFound);

    // If you got this far, then assume Day of Month is 1 (first of the
    // month)
    if (dom == invalidDate) {
      // No date found, resolution is month
      dom = 1;
    } else if (dom == 0) {
      anno.setValid(false);
      return;
    } else {
      // dt.resolution = DateMatch.TimeResolution.DAY;
    }
    // Normalizer Time fields found, H, M, s.SSS, etc.
    //
    cal = cal.withDayOfMonth(dom);
    // For normal M/D/Y patterns, set the default time to noon, UTC
    // Overall, we want to ensure that the general yyyy-mm-dd form is not
    // impacted
    // by time zone and default hour of 00:00; -- this generally would yield
    // a date format a day early for ALL US timezones.
    //
    // Time res: the presence of a FIELD, hh, mm, or ss means the pattern
    // has that level of resolution.
    // So even if time is 00:00:00Z -- all zeroes -- the resolution is still
    // SECONDS.
    //
    int hour = normalizeTime(elementsFound, "hh");
    if (hour >= 0) {
      // Only if HH:MM... is present do we try to detect TZ.
      //
      DateTimeZone tz = normalizeTZ(elementsFound);
      if (tz != null) {
        cal = cal.withZone(tz);
      }
      // NON-zero hour.
      // dt.resolution = DateMatch.TimeResolution.HOUR;
      int min = normalizeTime(elementsFound, "mm");
      if (min >= 0) {
        // dt.resolution = DateMatch.TimeResolution.MINUTE;
        // NON-zero minutes
        cal = cal.withHourOfDay(hour);
        cal = cal.withMinuteOfHour(min);
      } else {
        // No minutes
        cal = cal.withHourOfDay(hour);
      }
    } else {
      // No hour; default is 12:00 UTC.
      cal = cal.withHourOfDay(12);
    }
    // dt.datenorm = new Date(_cal.getMillis());
    // dt.datenorm_text = DateNormalization.format_date(dt.datenorm);
    normalizedResults.put("hierarchy", "Time.date");
    normalizedResults.put("isEntity", true);
    normalizedResults.put("date", cal);
    return;
  }

  /**
   * Z or Zulu is not always recognized as UTC / GMT+0000
   */
  public static DateTimeZone normalizeTZ(java.util.Map<String, String> elements) {
    if (elements.containsKey("SHORT_TZ")) {
      String tz = elements.get("SHORT_TZ");
      if ("Z".equalsIgnoreCase(tz)) {
        return DateTimeZone.UTC;
      }
      return DateTimeZone.forID(tz);
    } else if (elements.containsKey("LONG_TZ")) {
      String tz = elements.get("LONG_TZ");
      if ("Zulu".equalsIgnoreCase(tz)) {
        return DateTimeZone.UTC;
      }
      return DateTimeZone.forID(tz);
    }
    // Default is UTC+0.
    return null;
  }

  /**
   * Given a FIELD hh, mm, or ss, get FIELD from map and normalize/validate the value
   */
  public static int normalizeTime(java.util.Map<String, String> elements, String tmField) {
    if (!elements.containsKey(tmField)) {
      return -1;
    }
    int val = getIntValue(elements.get(tmField));
    if (val < 0) {
      return -1;
    }
    if ("hh".equals("tmField")) {
      if (val < 24) {
        return val;
      }
    } else if ("mm".equals("tmField")) {
      if (val < 60) {
        return val;
      }
    } else if ("ss".equals("tmField")) {
      if (val < 60) {
        return val;
      }
    } else {
      // Unknown FIELD;
      return val;
    }
    return -1;
  }

  /**
   * @param elements
   * @return
   */
  public static int normalizeYear(java.util.Map<String, String> elements) {
    // YEAR yyyy
    // YY yy
    // YEARYY yy or yyyy
    String year = elements.get("YEAR");
    // boolean _is_4digit = false;
    boolean isYear = false;
    if (year != null) {
      // year = yy;
      return getIntValue(year);
    }
    int yearValue = invalidDate;
    String yy = elements.get("YY");
    String yearyy = elements.get("YEARYY");
    if (yy != null) {
      yearValue = getIntValue(yy);
      // NOTE: because we matched a YY FIELD, this should ideally be in
      // an explicity format.
      isYear = true;
    } else if (yearyy != null) {
      if (yearyy.startsWith("'")) {
        isYear = true;
        yearyy = yearyy.substring(1);
      }
      if (yearyy.length() == 4) {
        // _is_4digit = true;
        isYear = true;
      } else if (yearyy.length() == 2) {
        // Special case: 00 yields 2000
        // this check here has no effect, as rule below considers "00" =
        // 0, which is < FUTURE_YY_THRESHOLD
        if ("00".equals(yearyy)) {
          isYear = true;
        }
      } else {
        yearValue = invalidDate;
      }
      yearValue = getIntValue(yearyy);
    }
    if (yearValue == invalidDate) {
      return invalidDate;
    }
    if (yearValue <= FUTURE_YY_THRESHOLD) {
      // TEST: '12, '13, ... '15 == yield 2012, 2013, 2015 etc.
      // limit is deteremined by current year + fuzzy limit.
      // is '18 2018 or 1918? What is your YY limit?
      //
      yearValue += MILLENIUM;
    } else if (yearValue <= 99 && isYear) {
      // Okay we got something beyond the threshold but is previous
      // century likely
      // '21 => 1921
      // '44 => 1944
      // 44 =>?
      yearValue += 1900;
    } else if (!isYear && yearValue > 31 && yearValue <= 99) {
      // Okay its NOT a year
      // its NOT a month
      // so "44" => 1944 is best guess. not 1844, not 0044...
      //
      yearValue += 1990;
    } else if (!isYear) {
      // Given two digit year that is possible day of month,... ignore!
      // JUN 17 -- no year given
      // JUN '17 -- is a year
      return invalidDate;
    }
    return yearValue;
  }

  /**
   * @param elements
   * @return
   * @throws java.text.ParseException
   */
  public static int normalizeMonth(java.util.Map<String, String> elements) {
    // MM, MONTH -- numeric 01-12
    // MON_ABBREV, MON_NAME -- text
    //
    String mm = elements.get("MM");
    String mon = elements.get("MONTH");
    int m = invalidDate;
    if (mm != null) {
      m = getIntValue(mm);
    } else if (mon != null) {
      m = getIntValue(mon);
    }
    if (m <= 12) {
      return m;
    }
    return invalidDate;
  }

  /**
   * @param elements
   * @return
   * @throws java.text.ParseException
   */
  public static int normalizeMonthName(java.util.Map<String, String> elements) {
    // MM, MONTH -- numeric 01-12
    // MON_ABBREV, MON_NAME -- text
    //
    String abbrev = elements.get("MON_ABBREV");
    String name = elements.get("MON_NAME");
    String text = null;
    if (abbrev != null) {
      text = abbrev;
    } else if (name != null) {
      text = name;
    } else {
      return invalidDate;
    }
    // How long is a month name really? May is shortest, Deciembre or
    // September are longest.
    if (text.length() < 3 || text.length() > 10) {
      return invalidDate;
    }

    DateTime mon = MONTH_FORMAT.parseDateTime(text);
    return mon.getMonthOfYear();
  }

  /**
   * @param elements
   * @return
   */
  public static int normalizeDay(java.util.Map<String, String> elements) {
    int day = invalidDate;
    if (elements.containsKey("DOM")) {
      // DOM, DD -- numeric
      int dom = getIntValue(elements.get("DOM"));
      if (dom != invalidDate) {
        day = dom;
      }
    } else if (elements.containsKey("DD")) {
      int dd = getIntValue(elements.get("DD"));
      if (dd != invalidDate) {
        day = dd;
      }
    }
    if (day <= 31 && day >= 0) {
      return day;
    }
    return invalidDate;
  }

  /**
   * @param val
   * @return
   */
  public static int getIntValue(String val) {
    if (val != null) {
      try {
        return Integer.parseInt(val);
      } catch (Exception parseErr) {
        return invalidDate;
      }
    }
    return invalidDate;
  }

}
