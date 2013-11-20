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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opensextant.geodesy.MGRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * *
 * @author ubaldino
 */
public class MGRSParser {

  private MGRSParser() {
  }

  // Log object
  private static Logger log = LoggerFactory.getLogger(MGRSParser.class);

  /**
   * @param elements
   * @return
   */
  public static List<MGRS> parseMGRS(Map<String, String> elements) {

    // get all of the pieces
    String zone = elements.get("MGRSZone");
    String quad = elements.get("MGRSQuad");
    String en = elements.get("Easting_Northing");

    // have all the pieces?
    if (zone == null || quad == null || en == null) {
      return null;
    }

    // the results
    List<MGRS> mgrs = new ArrayList<MGRS>();

    // look for alternate interpretations
    List<String> variants = findVariants(zone, quad, en);

    //get the MGRS for each possibility and add to results
    for (String var : variants) {
      if (isOK(var)) {
        MGRS tmp = null;
        try {
          tmp = new MGRS(var);
        } catch (IllegalArgumentException e) {
          log.warn("Could not parse MGRS:" + var);
        }
        if (tmp != null) {
          mgrs.add(tmp);
        }
      }
    }

    return mgrs;
  }

  private static List<String> findVariants(String zn, String quad, String eastNorth) {
    List<String> vars = new ArrayList<String>();

    // form the whole string
    String text = deleteWhitespace(zn + quad + eastNorth);

    vars.add(text);

    return vars;

    // TODO verify and cleanup the alternate form stuff below
    /*
     * // ---------------------------------------| // // MGRS precision is 1m. Quad is 100,000m sq so resolution is 5
     * digits + // 5 digits with optional whitespace // 99999n 99999e -- in MGRS we never see "m" units or N/E denoted
     * // explicitly // Occassionally, newlines or whitespace are interspersed in offset // minimal: // dd // ddddd
     * ddddd with an additional one or two white spaces. The offsets // start and end with numbers. Only whitespace
     * between is optional. // ddddd dddddd additional digit in Easting -- trailing 6th digit is a // typo; trim off //
     * dddddd ddddd additional digit in Northing -- trailing 6th digit is a // typo; trim off // ddddddddddd Typo
     * introduces ambiguity -- only correct thing is to // split on halfway point +/- 1 digit and emit two answers //
     * dd\nddd ddddd Newline early in offset // ---------------------------------------| Integer digits =
     * countDigits(en); boolean isOddLen = ((digits & 0x0001) == 1); //cleanup the pieces String z =
     * deleteWhitespace(zn); String q = deleteWhitespace(quad); String en = deleteWhitespace(eastNorth); if (!isOddLen)
     * { } else { } // if normal // if eatNoth has odd digits // ---------------------------- // Slightly obscure case
     * that is possibly a typo or Easting/Northing // disturbed. // // The following logic for parsing is predominantly
     * related to // managing typos and rare cases. // < 5% of the instances seen fall into this category. // //
     * ---------------------------- int spaceCount = countWhiteSpace(en); String nenorm; StringBuilder mgrs1 = null; if
     * (spaceCount == 0) { nenorm = en; // ddddddddd odd number of digits, no spaces. // answer 1: dddd ddddd ==>
     * N=dddd0 // answer 2: ddddd dddd ==> E=dddd0 int midpoint = nenorm.length() / 2; mgrs1 = new StringBuilder(en);
     * mgrs1.insert(midpoint, "0"); // N=dddd0, add 0 mgrs1.insert(0, quad); mgrs1.insert(0, zone); StringBuilder mgrs2
     * = new StringBuilder(en); mgrs2.append("0"); // E=dddd0 add 0 mgrs2.insert(0, quad); mgrs2.insert(0, zone); try {
     * MGRS m1 = new MGRS(mgrs1.toString()); mgrs.add(m1); } catch (IllegalArgumentException e) {
     * log.debug("Could not parse MGRS:" + mgrs1); } try { MGRS m2 = new MGRS(mgrs2.toString()); mgrs.add(m2); } catch
     * (IllegalArgumentException e) { log.debug("Could not parse MGRS:" + mgrs2); } return mgrs; } nenorm =
     * squeezeWhitespace(en); spaceCount = countWhiteSpace(nenorm); int wsIndex = nenorm.indexOf(" "); int midpoint =
     * nenorm.length() / 2; // Even Split -- meaning easting northing appear to be good. But one // needs to be fixed.
     * // boolean even_split = Math.abs( midpoint - ws_index ) <= 1; // Given one of // dddd ddddd // ddddd dddd // dd
     * ddddddd // where whitespace is ' ' or '\n' or '\r', etc. // GIVEN: dddd ddddd if (spaceCount == 1 && (wsIndex +
     * 1) == midpoint) { mgrs1 = new StringBuilder(nenorm); // ANSWER: dddd0 ddddd mgrs1.insert(wsIndex, "0");
     * mgrs1.insert(0, quad); mgrs1.insert(0, zone); // Just one answer: MGRS tmp = new
     * MGRS(deleteWhitespace(mgrs1.toString())); mgrs.add(tmp); return mgrs; } if (spaceCount == 1 && (wsIndex ==
     * midpoint)) { mgrs1 = new StringBuilder(nenorm); // ANSWER: ddddd dddd0 mgrs1.append("0"); mgrs1.insert(0, quad);
     * mgrs1.insert(0, zone); MGRS tmp = new MGRS(deleteWhitespace(mgrs1.toString())); mgrs.add(tmp); return mgrs; } //
     * Given // ddd dd d // ddddd ddd dd // etc. // You have a bunch of MGRS digits broken up by whitespace. // This is
     * really obscure case where formatting or content // conversion // or word processing interferred with the MGRS
     * text. // // This is < 0.1% of the cases // nenorm = deleteWhitespace(en); // ddddddddd odd number of digits, no
     * spaces. // answer 1: dddd ddddd ==> N=dddd0 // answer 2: ddddd dddd ==> E=dddd0 midpoint = nenorm.length() / 2;
     * mgrs1 = new StringBuilder(nenorm); mgrs1.insert(midpoint, "0"); // N=dddd0, add 0 mgrs1.insert(0, quad);
     * mgrs1.insert(0, zone); StringBuilder mgrs2 = new StringBuilder(nenorm); mgrs2.append("0"); // E=dddd0 add 0
     * mgrs2.insert(0, quad); mgrs2.insert(0, zone); MGRS m1 = new MGRS(mgrs1.toString()); MGRS m2 = new
     * MGRS(mgrs2.toString()); mgrs.add(m1); mgrs.add(m2); return mgrs; return null;
     */
  }

  private static boolean isOK(String txt) {
// TODO add reject patterns
    // possible reject patterns
    // 12 DEG 1234 - obscure lat/lon
    // 23 JAN 1900 - dates

    return true;
  }

  /**
   * @param x
   * @return
   */
  protected static int parseInt(String x) {
    try {
      return Integer.parseInt(x);
    } catch (Exception e) {
      return -1;
    }
  }

  static final Pattern DELWS = Pattern.compile("\\s+");

  public static String deleteWhitespace(String t) {
    Matcher m = DELWS.matcher(t);
    if (m != null) {
      return m.replaceAll("");
    }
    return t;
  }

  /**
   * Counts all digits in text.
   * @param txt
   * @return
   */
  public static int countDigits(String txt) {
    if (txt == null) {
      return 0;
    }
    int digits = 0;
    for (char c : txt.toCharArray()) {
      if (Character.isDigit(c)) {
        ++digits;
      }
    }
    return digits;
  }

  /**
   * Counts all digits in text.
   * @param txt
   * @return
   */
  public static int countWhiteSpace(String txt) {
    if (txt == null) {
      return 0;
    }
    int ws = 0;
    for (char c : txt.toCharArray()) {
      // isWhitespaceChar(c)?
      if (Character.isWhitespace(c)) {
        ++ws;
      }
    }
    return ws;
  }

  /**
   * Minimize whitespace.
   * @param t
   * @return String
   */
  public static String squeezeWhitespace(String t) {
    Matcher m = DELWS.matcher(t);
    if (m != null) {
      return m.replaceAll(" ");
    }
    return t;
  }
}
