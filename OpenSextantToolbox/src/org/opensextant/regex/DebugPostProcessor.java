package org.opensextant.regex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugPostProcessor extends PostProcessorBase {


  // Log object
  private static Logger log = LoggerFactory.getLogger(DebugPostProcessor.class);
  
  
  @Override
  public Comparator<? super RegexAnnotation> getComparator() {
    return new PositionComparator();
  }
  
  @Override
  public List<RegexAnnotation> decide(List<RegexAnnotation> inters) {
    List<RegexAnnotation> keepers = new ArrayList<RegexAnnotation>();

    if (inters != null && inters.size() > 0) {
      // sort the annotations by document order
      Collections.sort(inters, this.getComparator());
      
      // build up a message showing the alternative annotations
      StringBuffer tmp = new StringBuffer();
      tmp.append("Must decide amongst these " + inters.size() + " annotations\n");
      for(RegexAnnotation a : inters){
        tmp.append("\t"+ a.toString() + "\n");
      }
      tmp.append("-------\n");
      log.debug(tmp.toString());

      // return them all
      keepers.addAll(inters);
    }
    return keepers;
  }
  
  
  
  
}
