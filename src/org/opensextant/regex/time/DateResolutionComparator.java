/*
 Copyright 2009-2013 The MITRE Corporation.
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 * **************************************************************************
 *                          NOTICE
 * This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 **/
package org.opensextant.regex.time;

import java.util.Comparator;

import org.opensextant.regex.RegexAnnotation;
import org.opensextant.regex.time.DateTimeNormalizer2.TimeResolution;

/**
 * The Class DateResolutionComparator.<br>
 * This comparator can be used to sort RegexAnnotation of type Date,Time and
 * DayOfTheMonth by their temporal resolution
 */
public class DateResolutionComparator implements Comparator<RegexAnnotation> {

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(RegexAnnotation a1, RegexAnnotation a2) {
		int result;

		String type1 = a1.getType();
		String type2 = a2.getType();

		if (!type1.matches("Date|Time|DayOfTheMonth") || !type2.matches("Date|Time|DayOfTheMonth")) {
			return 0;
		}

		// get the two temporal resolutions
		TimeResolution res1 = (TimeResolution) a1.getFeatures().get("precision");
		TimeResolution res2 = (TimeResolution) a2.getFeatures().get("precision");

		// if Date annos have not been normalized, there will be no "precision"
		// value
		if (res1 == null) {
			res1 = TimeResolution.NONE;
		}

		if (res2 == null) {
			res2 = TimeResolution.NONE;
		}

		// compare temporal resolutions, highest first
		result = res2.compareTo(res1);

		// if resolutions are equal, compare rule families,
		// TODO add family ranking? e.g. MY vs DayOfTheMonth (jan 13)
		// TODO add type (date,time,dayofweek) ranking?

		// if resolutions and family are equal, compare lengths, select the
		// longer
		if (result == 0) {
			long len1 = a1.getEnd() - a1.getStart();
			long len2 = a2.getEnd() - a2.getStart();
			result = Long.compare(len2, len1);
		}

		return result;
	}

}
