/**
 *
 *  Copyright 2009-2013 The MITRE Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * **************************************************************************
 *                          NOTICE
 * This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 */
package org.opensextant.regex.geo;

import java.util.HashMap;
import java.util.Map;

/**
 * Ordinate represents all the various fields a WGS84 cartesian coordinate could have. degree/minute/second, as well as
 * fractional minutes and fractional seconds. * Patterns may have symbols which further indicate if a pattern is a
 * literal decimal number (e.g., 33-44, 33.44, 33.444, 33-4444) of if the numbers are in minutes/seconds units (33:44).
 * *
 * @author ubaldino
 */
public final class OrdinateParser {

  // Log object
  // private static Logger log = LoggerFactory.getLogger(Ordinate.class);

  // the two type of Ordinates
  public enum ORDINATE_TYPE {
    DD, DMS
  };
  // the two axes
  public enum AXIS {
    LATITUDE, LONGITUDE
  };

  public static final int LAT_MAX = 90;
  public static final int LON_MAX = 180;

  // public static final String[] COORDINATE_SYMBOLS = {"��", "��", "'", "\"", ":", "lat", "lon", "geo", "coord",
  // "deg"};

  public static final String WEST = "W";
  public static final String SOUTH = "S";
  public static final String NORTH = "N";
  public static final String EAST = "E";
  public static final String NEGATIVE = "-";
  public static final String POSITIVE = "+";

  // public static final int NO_HEMISPHERE_VALUE = -0x10;
  public static final int POS_HEMI = 1;
  public static final int NEG_HEMI = -1;
  public static final int NO_HEMI = 0;

  public static final Map<String, Integer> HEMI_MAP = new HashMap<String, Integer>();
  static {
    HEMI_MAP.put(WEST, NEG_HEMI);
    HEMI_MAP.put(SOUTH, NEG_HEMI);
    HEMI_MAP.put(EAST, POS_HEMI);
    HEMI_MAP.put(NORTH, POS_HEMI);
    HEMI_MAP.put(NEGATIVE, NEG_HEMI);
    HEMI_MAP.put(POSITIVE, POS_HEMI);
  }

  private OrdinateParser() {

  }

  public static Ordinate parse(Map<String, String> elements, AXIS ax, OrdinateParser.ORDINATE_TYPE type) {

    Ordinate ord = new Ordinate();
    ord.setType(type);
    ord.setAxis(ax);

    if (ax == AXIS.LATITUDE) {
      parseLatitude(ord, elements);
      parseLatitudeHemisphere(ord, elements);
    } else {
      parseLongitude(ord, elements);
      parseLongitudeHemisphere(ord, elements);
    }

    if (ord.isValid()) {
      return ord;
    } else {
      return null;
    }

  }

  /**
   * @return
   */
  private static final boolean parseLatitude(Ordinate ord, Map<String, String> elements) {
    // DEGREES
    Double degrees = null;
    Integer deg = getIntValue(elements.get("degLat"));
    Integer deg2 = getIntValue(elements.get("dmsDegLat"));
    Float deg3 = getDecimalValue(elements.get("decDegLat"));
    if (deg != null) {
      degrees = 1.0 * deg;
    } else if (deg2 != null) {
      degrees = 1.0 * deg2;
    } else if (deg3 != null) {
      degrees = 1.0 * deg3;
    } else {
      return false;
    }
    // MINUTES
    Double minutes = null;
    Integer min = getIntValue(elements.get("minLat"));
    Integer min2 = getIntValue(elements.get("dmsMinLat"));
    Float min3 = getDecimalValue(elements.get("decMinLat"));
    Float min3dash = getDecimalValue(elements.get("decMinLat3"));
    if (min != null) {
      minutes = 1.0 * min;
    } else if (min2 != null) {
      minutes = 1.0 * min2;
    } else if (min3 != null) {
      minutes = 1.0 * min3;
    } else if (min3dash != null) {
      minutes = 1.0 * min3dash;
    }
    Float minFract = getFractionValue(elements.get("fractMinLat"));
    Float minFract2 = getFractionValue(elements.get("fractMinLat3")); // variation
    // 2, is a 3-digit orlongerfraction
    if (minFract != null) {
      minutes += minFract.floatValue();
    } else if (minFract2 != null) {
      minutes += minFract2.floatValue();
    }
    // SECONDS
    Double seconds = null;
    Integer sec = getIntValue(elements.get("secLat"));
    Integer sec2 = getIntValue(elements.get("dmsSecLat"));
    if (sec != null) {
      seconds = 1.0 * sec;
    } else if (sec2 != null) {
      seconds = 1.0 * sec2;
    }
    Float fsec = getFractionValue(elements.get("fractSecLat"));
    Float fsec2 = getFractionValue(elements.get("fractSecLatOpt"));
    if (fsec != null) {
      seconds += fsec.floatValue();
    } else if (fsec2 != null) {
      seconds += fsec2.floatValue();
    }

    if (degrees != null) {
      ord.setDegrees(degrees);
      if (minutes != null) {
        ord.setMinutes(minutes);
        if (seconds != null) {
          ord.setSeconds(seconds);
        }
      }

    }

    return true;
  }

  /**
   * This is a copy of the logic for digest_latitude_match; All I replace is "Lat" with "Lon"
   * @return
   */
  private static boolean parseLongitude(Ordinate ord, Map<String, String> elements) {
    // DEGREES
    Double degrees = null;
    Integer deg = getIntValue(elements.get("degLon"));
    Integer deg2 = getIntValue(elements.get("dmsDegLon"));
    Float deg3 = getDecimalValue(elements.get("decDegLon"));
    if (deg != null) {
      degrees = 1.0 * deg;
    } else if (deg2 != null) {
      degrees = 1.0 * deg2;
    } else if (deg3 != null) {
      degrees = 1.0 * deg3;
    } else {
      return false;
    }
    // MINUTES
    Double minutes = null;
    Integer min = getIntValue(elements.get("minLon"));
    Integer min2 = getIntValue(elements.get("dmsMinLon"));
    Float min3 = getDecimalValue(elements.get("decMinLon"));
    Float min3dash = getDecimalValue(elements.get("decMinLon3"));
    if (min != null) {
      minutes = 1.0 * min;
    } else if (min2 != null) {
      minutes = 1.0 * min2;
    } else if (min3 != null) {
      minutes = 1.0 * min3;
    } else if (min3dash != null) {
      minutes = 1.0 * min3dash;
    }
    Float minFract = getFractionValue(elements.get("fractMinLon"));
    Float minFract2 = getFractionValue(elements.get("fractMinLon3")); // variation
    // 2, is
    // a
    // 3-digit
    // or
    // longer
    // fraction
    if (minFract != null) {
      minutes += minFract.floatValue();
    } else if (minFract2 != null) {
      minutes += minFract2.floatValue();
    }
    // SECONDS
    Double seconds = null;
    Integer sec = getIntValue(elements.get("secLon"));
    Integer sec2 = getIntValue(elements.get("dmsSecLon"));
    if (sec != null) {
      seconds = 1.0 * sec;
    } else if (sec2 != null) {
      seconds = 1.0 * sec2;
    }
    Float fsec = getFractionValue(elements.get("fractSecLon"));
    Float fsec2 = getFractionValue(elements.get("fractSecLonOpt"));
    if (fsec != null) {
      seconds += fsec.floatValue();
    } else if (fsec2 != null) {
      seconds += fsec2.floatValue();
    }

    if (degrees != null) {
      ord.setDegrees(degrees);
      if (minutes != null) {
        ord.setMinutes(minutes);
        if (seconds != null) {
          ord.setSeconds(seconds);
        }
      }
    }

    return true;
  }

  private static void parseLatitudeHemisphere(Ordinate ord, java.util.Map<String, String> elements) {
    String hemiLat = elements.get("hemiLat");
    String hemiSignLat = elements.get("hemiLatSign");
    String hemiLatPre = elements.get("hemiLatPre");

    // default to psoitive
    int z = POS_HEMI;
    if (hemiLatPre != null) {
      z = getHemisphereSign(hemiLatPre);
    } else if (hemiLat != null) {
      z = getHemisphereSign(hemiLat);
    } else if (hemiSignLat != null) {
      z = getHemisphereSign(hemiSignLat);
    }
    ord.setHemi(z);

    return;
  }

  private static void parseLongitudeHemisphere(Ordinate ord, java.util.Map<String, String> elements) {
    String hemiLon = elements.get("hemiLon");
    String hemiSignLon = elements.get("hemiLonSign");
    String hemiLonPre = elements.get("hemiLonPre");

    int z = POS_HEMI;
    if (hemiLonPre != null) {
      z = getHemisphereSign(hemiLonPre);
    } else if (hemiLon != null) {
      z = getHemisphereSign(hemiLon);
    } else if (hemiSignLon != null) {
      z = getHemisphereSign(hemiSignLon);
    }
    ord.setHemi(z);

    return;
  }

  private static Integer getIntValue(String val) {
    if (val == null) {
      return null;
    }
    return new Integer(val);
  }

  /**
   * Convert numbers like "8.888" or "8-888" to decimal numbers.
   */
  private static Float getDecimalValue(String val) {
    if (val == null) {
      return null;
    }
    if (val.contains("-")) {
      // Log this situation
      val = val.replaceFirst("-", ".");
    }

    return new Float(val);
  }

  private static Float getFractionValue(String val) {
    if (val == null) {
      return null;
    }
    if (val.startsWith("-")) {
      // Log this situation
      val = val.replaceFirst("-", ".");
    } else if (!val.startsWith(".")) {
      // already has a decimal point?
      val = "." + val;
    }

    return Float.parseFloat(val);
  }

  public static int getHemisphereSign(String val) {
    if (val == null) {
      return NO_HEMI;
    }
    Integer s = HEMI_MAP.get(val.trim().toUpperCase());
    if (s != null) {
      return s.intValue();
    }
    return NO_HEMI;
  }
}
