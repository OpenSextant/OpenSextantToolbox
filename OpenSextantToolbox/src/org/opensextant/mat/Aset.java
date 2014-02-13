package org.opensextant.mat;

import java.util.ArrayList;
import java.util.List;

public class Aset {

  private Boolean hasID = false;
  private Boolean hasSpan = true;
  private String type = "NoType";
  private List<Attr> attrs = new ArrayList<Attr>();
  private List<Annot> annots = new ArrayList<Annot>();

  public Boolean getHasID() {
    return hasID;
  }

  public void setHasID(Boolean hasID) {
    this.hasID = hasID;
  }

  public Boolean getHasSpan() {
    return hasSpan;
  }

  public void setHasSpan(Boolean hasSpan) {
    this.hasSpan = hasSpan;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<Attr> getAttrs() {
    return attrs;
  }

  public void setAttrs(List<Attr> attrs) {
    this.attrs = attrs;
  }

  public List<Annot> getAnnots() {
    return annots;
  }

  public void setAnnots(List<Annot> annots) {
    this.annots = annots;
  }

  public void addAnnotation(Annot a) {
    this.annots.add(a);
  }

}
