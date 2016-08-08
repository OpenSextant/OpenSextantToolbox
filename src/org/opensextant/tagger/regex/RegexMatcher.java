package org.opensextant.tagger.regex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexMatcher {
	/** The rules used by this RegexMatcher. */
	List<RegexRule> rules = new ArrayList<RegexRule>();

	/** The list of entity type this matcher can find. */
	Set<String> types = new HashSet<String>();

	/** The postprocessors to apply. */
	Map<PostProcessor, Set<String>> posters = new HashMap<PostProcessor, Set<String>>();

	/**
	 * Future stuff: named groups useful? String namedGroup =
	 * "\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>"; Pattern namedGroupPattern =
	 * Pattern.compile(namedGroup); the pattern of a DEFINE within a RULE e.g
	 * "<somePiece>" NOTE: should we look for existing (not DEFINEDd) capture
	 * groups
	 */
	String elementRegex = "<[a-zA-Z0-9_]+>";
	Pattern elementPattern = Pattern.compile(elementRegex);

	/** Has this mather been sucessfully initialized. */
	boolean isInited;
	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(RegexMatcher.class);

	public RegexMatcher(URL patternFile) {
		initialize(patternFile);
	}

	public RegexMatcher(File patternFile) {
		initialize(patternFile);
	}

	public List<RegexAnnotation> match(String input) {

		// The matches to return
		List<RegexAnnotation> matches = new ArrayList<RegexAnnotation>();

		if (!isInited) {
			LOGGER.error("Tried to use RegexMatcher without initializing first");
			return matches;
		}

		for (RegexRule r : rules) {
			String t = r.getEntityType();
			Normalizer normer = r.getNormalizer();
			// Do the matching, looping over the rules
			Matcher matcher = r.getPattern().matcher(input);
			while (matcher.find()) {
				// for each hit from the regex, create a RegexAnnotation
				RegexAnnotation tmp = new RegexAnnotation(t, matcher.group(0), matcher.start(), matcher.end());
				// if the a normalizer has been specified,
				if (normer != null) {
					normer.normalize(tmp, r, matcher.toMatchResult());
				}
				// check to see if the normalizer declared the match invalid
				if (tmp.isValid()) {
					// add the "hierarchy" and "isEntity" features
					String tmpHier = r.getTaxo();
					if (tmpHier != null && tmpHier.trim().length() > 0) {
						tmp.getFeatures().put("hierarchy", tmpHier);
						tmp.getFeatures().put("isEntity", true);
					}

					tmp.setRule(r.getRuleFamily() + "-" + r.getRuleName());
					matches.add(tmp);
				}
			}
		}

		// run the matches through the postprocessor(s) if any specified
		for (PostProcessor p : posters.keySet()) {
			p.postProcess(matches, posters.get(p));
		}

		return matches;
	}

	public void initialize(URL patFile) {
		// the #DEFINE statements as name and regex
		Map<String, String> defines = new HashMap<String, String>();

		// the #NORM statements as entitytype and classname
		Map<String, String> normalizerClassnames = new HashMap<String, String>();

		// the #POST statements as entitytype and classname
		Map<String, Set<String>> posterClassnames = new HashMap<String, Set<String>>();

		// the #TAXO statements as entitytype and taxonomy string
		Map<String, String> taxos = new HashMap<String, String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(patFile.openStream(), "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			LOGGER.error("Error when opening pattern file", e1);
			return;
		} catch (IOException e1) {
			LOGGER.error("Error when opening pattern file", e1);
			return;
		}

		String line = null;
		String[] fields;
		while (true) {
			try {
				line = reader.readLine();
			} catch (IOException e) {
				LOGGER.error("Error when reading pattern file.", e);
				return;
			}
			if (line == null) {
				break;
			}
			line = line.trim();
			// Is it a define statement?
			if (line.startsWith("#DEFINE")) {
				// line should be
				// #DEFINE<tab><defineName><tab><definePattern>
				fields = line.split("[\t ]+", 3);
				defines.put(fields[1].trim(), fields[2].trim());
			} else if (line.startsWith("#RULE")) {// Is it a rule statement?
				// line should be
				// #RULE<tab><entityType><tab><rule_fam><tab><rule_name><tab><pattern>
				fields = line.split("[\t ]+", 5);
				String type = fields[1].trim();
				String fam = fields[2].trim();
				String ruleName = fields[3].trim();
				String rulePattern = fields[4].trim();
				RegexRule tmpRule = new RegexRule();
				tmpRule.setEntityType(type);
				tmpRule.setRuleFamily(fam);
				tmpRule.setRuleName(ruleName);
				tmpRule.setPatternString(rulePattern);
				rules.add(tmpRule);
				types.add(type);
			} else if (line.startsWith("#NORM")) {
				fields = line.split("[\t ]+", 3);
				String type = fields[1].trim();
				normalizerClassnames.put(type, fields[2].trim());
			} else if (line.startsWith("#TAXO")) {
				fields = line.split("[\t ]+", 3);
				String type = fields[1].trim();
				taxos.put(type, fields[2].trim());
			} else if (line.startsWith("#POST")) {
				fields = line.split("[\t ]+", 3);
				String type = fields[1].trim();
				String posterName = fields[2].trim();
				if (!posterClassnames.containsKey(posterName)) {
					posterClassnames.put(posterName, new HashSet<String>());
				}
				posterClassnames.get(posterName).add(type);
			}

			// Ignore everything else
		} // end file read loop

		try {
			if (reader != null) {
				reader.close();
			}
		} catch (IOException e) {
			LOGGER.error("Error when closing pattern file.", e);
		}
		// defines,rules and classes should be completely populated
		// substitute all uses of DEFINE patterns within a RULE
		// with the DEFINE pattern surrounded by a numbered capture group
		for (RegexRule r : rules) {
			String tmpRulePattern = r.getPatternString();
			Matcher elementMatcher = elementPattern.matcher(tmpRulePattern);
			// find all of the element definitions within the pattern
			int groupNum = 1;
			// add the entity type as the name for group 0 (whole match)
			r.getElementMap().put(0, r.getEntityType());
			// find and replace any DEFINEd patterns, keeping track of the group
			// number
			// first find any DEFINEd patterns and stick it and its group number
			// into the
			// rule element map
			while (elementMatcher.find()) {
				int elementStart = elementMatcher.start();
				int elementEnd = elementMatcher.end();
				String elementName = tmpRulePattern.substring(elementStart + 1, elementEnd - 1);
				r.getElementMap().put(groupNum, elementName);
				groupNum++;
			}
			// now, replace each of the DEFINEd patterns with its regex
			// equivalent
			// wrapped in ( ), so it becomes a numbered capture group
			for (String tmpDefineName : defines.keySet()) {
				String tmpDefinePattern = "(" + defines.get(tmpDefineName) + ")";
				tmpDefineName = "<" + tmpDefineName + ">";
				tmpRulePattern = tmpRulePattern.replace(tmpDefineName, tmpDefinePattern);
			}
			// set the modified pattern on the rule and create the Pattern from
			// it
			r.setModifedPatternString(tmpRulePattern);
			r.setPattern(Pattern.compile(r.getModifedPatternString()));
			// resolve and attach the normalizer object
			if (normalizerClassnames.containsKey(r.getEntityType())) {
				String normClassName = normalizerClassnames.get(r.getEntityType());
				Normalizer normer;
				try {
					normer = (Normalizer) Class.forName(normClassName).newInstance();
				} catch (InstantiationException e) {
					normer = new NoOpNormalizer();
					LOGGER.error("Cannot instantiate a " + normClassName + ", using a No Op normalizer instead.", e);
				} catch (IllegalAccessException e) {
					normer = new NoOpNormalizer();
					LOGGER.error(
							"Cannot access a " + normClassName + " to create one, using a No Op normalizer instead.",
							e);
				} catch (ClassNotFoundException e) {
					normer = new NoOpNormalizer();
					LOGGER.error("Normalizer Class " + normClassName + " not found,using a No Op normalizer instead.",
							e);
				} catch (java.lang.ClassCastException e) {
					normer = new NoOpNormalizer();
					LOGGER.error("Class " + normClassName + " is not a Normalizer,using a No Op normalizer instead.",
							e);
				}
				r.setNormalizer(normer);
			} else { // nothing in file use NoOpNormalzer
				r.setNormalizer(new NoOpNormalizer());
			}
			// resolve and attach the taxonomic string object
			if (taxos.containsKey(r.getEntityType())) {
				r.setTaxo(taxos.get(r.getEntityType()));
			} else { // nothing in file
				r.setTaxo("");
			}
		} // end rule loop

		// create the postprocessors

		for (String p : posterClassnames.keySet()) {
			PostProcessor pstr;

			try {
				pstr = (PostProcessor) Class.forName(p).newInstance();
			} catch (InstantiationException e) {
				pstr = new NoOpPostProcessor();
				LOGGER.error("Cannot instantiate a " + p + ", using a No Op PostProcessor instead.", e);
			} catch (IllegalAccessException e) {
				pstr = new NoOpPostProcessor();
				LOGGER.error("Cannot access a " + p + ", using a No Op PostProcessor instead.", e);
			} catch (ClassNotFoundException e) {
				pstr = new NoOpPostProcessor();
				LOGGER.error("Class " + p + " is not a PostProcessor, using a No Op PostProcessor instead.", e);
			}

			posters.put(pstr, posterClassnames.get(p));
		}

		isInited = true;
	}

	/** End initialize. */

	public void initialize(File patFile) {
		try {
			initialize(patFile.toURI().toURL());
		} catch (MalformedURLException e) {
			LOGGER.error("Cannot initialize the matcher using pattern file  " + patFile.getName(), e);
		}
	}

	public List<RegexRule> getRules() {
		return rules;
	}

	public Set<String> getTypes() {
		return types;
	}

}
