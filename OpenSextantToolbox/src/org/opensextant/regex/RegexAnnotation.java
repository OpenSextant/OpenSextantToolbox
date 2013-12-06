package org.opensextant.regex;

import java.util.HashMap;
import java.util.Map;

public class RegexAnnotation {
  private int start;
  private int end;
  private String type;
  private String matchText;
  private String rule;
  private boolean valid = true;
  private Map<String, Object> features = new HashMap<String, Object>();

  public RegexAnnotation(String type, String text, int start, int end) {
    this.start = start;
    this.end = end;
    this.type = type;
    this.matchText = text;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getEnd() {
    return end;
  }

  public void setEnd(int end) {
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

  public String getRule() {
    return rule;
  }

  public void setRule(String rule) {
    this.rule = rule;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public Map<String, Object> getFeatures() {
    return features;
  }

  public void setFeatures(Map<String, Object> features) {
    this.features = features;
  }

  public boolean interactsWith(RegexAnnotation other){
    
    int s1 =this.start;
    int e1 = this.end;
    int s2 = other.start;
    int e2 = other.end;
    
    if( (s1 < s2 && e1 < s2) || (s2 < s1 && e2 < s1)  ){
      return false;
    }
    
    return true;
  }
  
  
  @Override
  public String toString() {
    return String.format("%s (%s %s %s %s)", matchText, type, start, end, features);
  }

}