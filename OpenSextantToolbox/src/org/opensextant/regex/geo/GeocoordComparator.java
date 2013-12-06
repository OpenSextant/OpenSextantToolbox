package org.opensextant.regex.geo;

import java.util.Comparator;

import org.opensextant.regex.RegexAnnotation;

public class GeocoordComparator  implements Comparator<RegexAnnotation> {

  @Override
  public int compare(RegexAnnotation o1, RegexAnnotation o2) {
    
    if (!o1.getType().equalsIgnoreCase("GEOCOORD") || !o2.getType().equalsIgnoreCase("GEOCOORD")) {
      return 0;
    }
    
    String fam1 = (String) o1.getFeatures().get("geoFamily");
    String fam2 = (String) o2.getFeatures().get("geoFamily");
    
    if(fam1.equalsIgnoreCase("DM") && fam2.equalsIgnoreCase("DMS")  ){
      return 1;
    }
    if(fam1.equalsIgnoreCase("DMS") && fam2.equalsIgnoreCase("DM")  ){
      return -1;
    }
    
    int len1 = o1.getEnd() - o1.getStart();
    int len2 = o2.getEnd() - o2.getStart();
    
    return Integer.compare(len1, len2);
   
  }

}
