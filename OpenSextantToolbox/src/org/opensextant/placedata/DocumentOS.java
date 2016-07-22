package org.opensextant.placedata;

import java.util.ArrayList;
import java.util.List;

public class DocumentOS {

  private String content;
  private List<AnnotationOS> annoList;

  public DocumentOS() {
    this.content = "";
    annoList = new ArrayList<AnnotationOS>();
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public List<AnnotationOS> getAnnoList() {
    return annoList;
  }

  public void setAnnoList(List<AnnotationOS> annoList) {
    this.annoList = annoList;
  }

  public void addAnno(AnnotationOS tmpAnno) {
    this.annoList.add(tmpAnno);

  }

  public String getSnippet(AnnotationOS an, int size) {
    int start = (int) (an.getStart() - size);
    int end = (int) (an.getEnd() + size);

    if (start < 0) {
      start = 0;
    }
    if (end > this.content.length()) {
      end = this.content.length();
    }

    return this.content.substring(start, end).replaceAll("[\n\r]+", " ");
  }

}
