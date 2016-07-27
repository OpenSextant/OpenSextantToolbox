package org.opensextant.service.processing;

import java.util.ArrayList;
import java.util.List;

public class DocumentBean {

  private String content;
  private List<Anno> annoList;

  public DocumentBean() {
    this.content = "";
    annoList = new ArrayList<Anno>();
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public List<Anno> getAnnoList() {
    return annoList;
  }

  public void setAnnoList(List<Anno> annoList) {
    this.annoList = annoList;
  }

  public void addAnno(Anno tmpAnno) {
    this.annoList.add(tmpAnno);

  }

  public String getSnippet(Anno an, int size) {
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
