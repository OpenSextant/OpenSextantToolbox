package org.opensextant.vocab;

import java.util.HashMap;
import java.util.Map;

public class Vocab {
  // internal id
  private String id;

  // the vocabulary phrase as it appeared in the lexicon/dictionary
  private String vocabMatch;

  // the collection,catalog or other grouping of vocabulary
  private String collection;

  // a category
  private String category;

  // a taxonomic categorization
  private String taxonomy;

  // any other attributes,characteristics or labels
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

  public String toString() {
    String tmp = this.getVocabMatch() + " (" + this.getCategory() + "/" + this.getTaxonomy() + ")";
    return tmp;
  }

}
