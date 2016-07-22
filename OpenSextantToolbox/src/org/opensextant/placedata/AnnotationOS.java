package org.opensextant.placedata;

import java.util.HashMap;
import java.util.Map;

public class AnnotationOS {

  private long start;
  private long end;
  private String type = "";
  private String matchText = "";
  private Map<String, Object> features = new HashMap<String, Object>();
  private boolean valid = true;

  public AnnotationOS(String type, String text, int start, int end) {
    this.start = start;
    this.end = end;
    this.type = type;
    this.matchText = text;
  }

  public AnnotationOS() {
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMatchText() {
    return matchText;
  }

  public void setMatchText(String matchText) {
    this.matchText = matchText;
  }

  public Map<String, Object> getFeatures() {
    return features;
  }

  public void setFeatures(Map<String, Object> features) {
    this.features = features;
  }

  public void addFeatures(String name, Object value) {
    this.features.put(name, value);
  }

  @Override
  public String toString() {
    return String.format("%s (%s %s %s %s)", matchText, type, start, end, features);
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public boolean interactsWith(AnnotationOS other) {

    long s1 = this.getStart();
    long e1 = this.getEnd();
    long s2 = other.getStart();
    long e2 = other.getEnd();

    return (s1 >= s2 || e1 >= s2) && (s2 >= s1 || e2 >= s1);
  }

}
