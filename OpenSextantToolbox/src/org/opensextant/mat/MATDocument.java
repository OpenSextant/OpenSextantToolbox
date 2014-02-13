package org.opensextant.mat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MATDocument {

  private Integer version = 2;
  private String signal;
  private List<Aset> asets = new ArrayList<Aset>();
  private Map<String, Object> metadata;

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getSignal() {
    return signal;
  }

  public void setSignal(String signal) {
    this.signal = signal;
  }

  public List<Aset> getAsets() {
    return asets;
  }

  public void setAsets(List<Aset> asets) {
    this.asets = asets;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

}
