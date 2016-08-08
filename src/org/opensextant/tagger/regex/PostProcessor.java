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
package org.opensextant.tagger.regex;

import java.util.List;
import java.util.Set;

import org.opensextant.tagger.Match;

/**
 * The Interface PostProcessor. <br>
 * This interface provides the contract for any type of processing done to the
 * collection of RegexAnnotations which has been found by the Matcher and
 * specified via the "#POST" element of a Matcher configuration file.
 */
public interface PostProcessor {

	/**
	 * Post process a list of RegexAnnotations. Post processing could include
	 * removing duplicates, selecting a single annotation from multiple ambigous
	 * possibilities, deriving new annotations from ones in the provided list
	 * ...
	 * 
	 * @param annos
	 *            the RegexAnnotations to post process
	 * @param types
	 *            the types of RegexAnnotations to process (implementation
	 *            specific)
	 */
	void postProcess(List<Match> annos, Set<String> types);

}