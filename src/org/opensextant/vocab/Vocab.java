package org.opensextant.vocab;

import java.util.HashMap;
import java.util.Map;

public class Vocab {
	/** Internal id. */
	private String id;

	/** The vocabulary phrase as it appeared in the lexicon/dictionary. */
	private String vocabMatch;

	/** The collection,catalog or other grouping of vocabulary. */
	private String collection;

	/** A category. */
	private String category;

	/** A taxonomic categorization. */
	private String taxonomy;

	/** Any other attributes,characteristics or labels. */
	private Map<String, Object> attributes = new HashMap<String, Object>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVocabMatch() {
		return vocabMatch;
	}

	public void setVocabMatch(String vocabMatch) {
		this.vocabMatch = vocabMatch;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getTaxonomy() {
		return taxonomy;
	}

	public void setTaxonomy(String taxonomy) {
		this.taxonomy = taxonomy;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String toString() {
		return getVocabMatch() + " (" + getCategory() + "/" + getTaxonomy() + ")";
	}

}
