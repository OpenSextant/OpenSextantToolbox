package org.opensextant.mat;

public class Attr {

  // enum AttrType {STRING,ANNOTATION,FLOAT,INT,BOOLEAN};
  // enum AggrType {NULL,NONE,LIST,SET};

  private String name;
  private String type;
  private String aggregation;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAggregation() {
    return aggregation;
  }

  public void setAggregation(String aggregation) {
    this.aggregation = aggregation;
  }

}
