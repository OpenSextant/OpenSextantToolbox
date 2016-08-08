package org.opensextant.tagger.regex;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexRule {
	private String entityType;
	private String ruleFamily;
	private String ruleName;
	private String patternString;
	private String modifedPatternString;
	private String taxo;
	private Pattern pattern;
	private Normalizer normalizer;
	private Map<Integer, String> elementMap = new HashMap<Integer, String>();

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getRuleFamily() {
		return ruleFamily;
	}

	public void setRuleFamily(String ruleFamily) {
		this.ruleFamily = ruleFamily;
	}

	public String getRuleName() {
		return ruleName;
	}

	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}

	public String getPatternString() {
		return patternString;
	}

	public void setPatternString(String patternString) {
		this.patternString = patternString;
	}

	public String getModifedPatternString() {
		return modifedPatternString;
	}

	public void setModifedPatternString(String modifedPatternString) {
		this.modifedPatternString = modifedPatternString;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public Normalizer getNormalizer() {
		return normalizer;
	}

	public void setNormalizer(Normalizer normalizer) {
		this.normalizer = normalizer;
	}

	public Map<Integer, String> getElementMap() {
		return elementMap;
	}

	public void setElementMap(Map<Integer, String> elementMap) {
		this.elementMap = elementMap;
	}

	public String getTaxo() {
		return taxo;
	}

	public void setTaxo(String taxo) {
		this.taxo = taxo;
	}

	@Override
	public String toString() {
		return this.entityType + "(" + getTaxo() + ") " + this.ruleFamily + "/" + this.ruleName + "->"
				+ this.patternString + "\t" + this.modifedPatternString + "\t" + this.elementMap + "\t"
				+ this.normalizer.getClass().getName();
	}
}
