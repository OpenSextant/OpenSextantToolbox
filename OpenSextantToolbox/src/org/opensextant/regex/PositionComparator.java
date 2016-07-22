package org.opensextant.regex;

import java.util.Comparator;

import org.opensextant.placedata.AnnotationOS;

public class PositionComparator implements Comparator<AnnotationOS> {

  public int compare(AnnotationOS a1, AnnotationOS a2) {
    int result;

    // compare start
    result = Long.compare(a1.getStart(), a2.getStart());

    // if starts are equal compare ends, longest first
    if (result == 0) {
      result = Long.compare(a2.getEnd(), a1.getEnd());
    }

    return result;
  }

}
