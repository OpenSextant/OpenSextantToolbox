package org.opensextant.vocab;

import java.util.ArrayList;
import java.util.List;

public class VocabMatch {

  // the vocabulary match as it appeared in the document
  private String textMatch;

  // the location this was found in the document
  private long start = 0L;
  private long end = 0L;

  // the vocab entries this has been matched to
  private List<Vocab> vocabs = new ArrayList<Vocab>();

  public String getTextMatch() {
    return textMatch;
  }

  public void setTextMatch(String textMatch) {
    this.textMatch = textMatch;
  }

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public long getEnd() {
    return end;
  }

  public void setEnd(long end) {
    this.end = end;
  }

  public List<Vocab> getVocabs() {
    return vocabs;
  }

  public void setVocabs(List<Vocab> vocabs) {
    this.vocabs = vocabs;
  }

  public void addVocab(Vocab v) {
    this.vocabs.add(v);
  }

}
