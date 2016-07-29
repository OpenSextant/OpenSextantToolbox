/*
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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opensextant.geodesy.MGRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * *.
 * 
 * @author ubaldino
 */
public class MGRSParser {

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MGRSParser.class);

	private static DateFormat df1 = new java.text.SimpleDateFormat("ddMMMyyyy");
	private static DateFormat df2 = new java.text.SimpleDateFormat("ddMMMyy");
	static final Pattern DELWS = Pattern.compile("\\s+");

	static {
		// turn off lenient date parsing
		df1.setLenient(false);
		df2.setLenient(false);
	}

	private MGRSParser() {
	}

	public static List<MGRS> parseMGRS(Map<String, String> elements) {

		// get all of the pieces
		String zone = elements.get("MGRSZone");
		String quad = elements.get("MGRSQuad");
		String en = elements.get("Easting_Northing");

		// the results
		List<MGRS> mgrs = new ArrayList<MGRS>();

		// have all the pieces?
		if (zone == null || quad == null || en == null) {
			return mgrs;
		}

		// look for alternate interpretations
		List<String> variants = findVariants(zone, quad, en);

		// get the MGRS for each possibility and add to results
		for (String var : variants) {
			if (!badMGRS(var)) {
				MGRS tmp = null;
				try {
					tmp = new MGRS(var);
				} catch (IllegalArgumentException e) {
					LOGGER.warn("Could not parse MGRS:" + var, e);
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

	}

	/** MGRS instances which should be rejected. */
	private static boolean badMGRS(String txt) {
		// reject these patterns
		// 23 JAN 1900 - date
		// 23 JAN 73 - date

		// TODO what about these?
		// 20PER1000 - ratio
		// 18DEG20 - part of an obscure lat/lon

		// remove whitespace
		String tmp = txt.replaceAll("\\s", "");
		Date dt = null;

		try {
			dt = df1.parse(tmp);
		} catch (ParseException e) {
			LOGGER.debug(tmp + " looks like a date not an MGRS");
		}

		if (dt != null) {
			LOGGER.info("Rejecting " + txt + " as bad MGRS: Looks like a date");
			return true;
		}

		try {
			dt = df2.parse(tmp);
		} catch (ParseException e) {
			LOGGER.debug(tmp + " looks like a date not an MGRS");
		}

		if (dt != null) {
			LOGGER.info("Rejecting " + txt + " as bad MGRS: Looks like a date");
			return true;
		}

		LOGGER.debug("Accepting " + txt + " as good MGRS");

		return false;
	}

	protected static int parseInt(String x) {

		try {
			return Integer.parseInt(x);
		} catch (NumberFormatException e) {
			LOGGER.error("Could parse integer:", e);
			return -1;
		}

	}

	public static String deleteWhitespace(String t) {
		Matcher m = DELWS.matcher(t);
		if (m != null) {
			return m.replaceAll("");
		}
		return t;
	}

	/**
	 * Counts all digits in text.
	 * 
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
	 * 
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
	 * 
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
