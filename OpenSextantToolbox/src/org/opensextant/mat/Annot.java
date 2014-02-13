package org.opensextant.mat;

import java.util.ArrayList;
import java.util.List;

public class Annot {

  private String id = null;
  private Long start = -1L;
  private Long end = -1L;
  private List<Object> attributeValues = new ArrayList<Object>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getStart() {
    return start;
  }

  public void setStart(Long start) {
    this.start = start;
  }

  public Long getEnd() {
    return end;
  }

  public void setEnd(Long end) {
    this.end = end;
  }

  public List<Object> getAttributeValues() {
    return attributeValues;
  }

  public void setAttributeValues(List<Object> attributeValues) {
    this.attributeValues = attributeValues;
  }

  public void addAttributeValue(Object value) {
    attributeValues.add(value);
  }

  public String toString() {

    String tmpID = this.getId();
    if (tmpID == null) {
      tmpID = "";
    }

    String tmp = tmpID + "(" + this.start + "," + this.end + ")";
    return tmp;
  }

}
