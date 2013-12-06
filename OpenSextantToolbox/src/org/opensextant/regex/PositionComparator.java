package org.opensextant.regex;

import java.util.Comparator;

public class PositionComparator implements Comparator<RegexAnnotation> {

  public int compare(RegexAnnotation a1, RegexAnnotation a2) {
    int result;

    // compare start 
    result = Integer.compare(a1.getStart(), a2.getStart());

    // if starts are equal compare ends, longest first
    if (result == 0) {
      result = Integer.compare(a2.getEnd(), a1.getEnd());
    }

    return result;
  }

}
