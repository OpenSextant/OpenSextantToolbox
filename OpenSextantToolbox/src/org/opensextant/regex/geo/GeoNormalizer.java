package org.opensextant.regex.geo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;

import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.MGRS;
import org.opensextant.geodesy.UTM;
import org.opensextant.placedata.AnnotationOS;
import org.opensextant.placedata.Geocoord;
import org.opensextant.regex.Normalizer;
import org.opensextant.regex.RegexRule;
import org.opensextant.regex.geo.OrdinateParser.AXIS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoNormalizer implements Normalizer {

  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(GeoNormalizer.class);

  /** There are 5 geocoord families. */
  private static final String DD_FAMILY = "DD";
  private static final String DM_FAMILY = "DM";
  private static final String DMS_FAMILY = "DMS";
  private static final String MGRS_FAMILY = "MGRS";
  private static final String UTM_FAMILY = "UTM";

  @Override
  public void normalize(AnnotationOS annotation, RegexRule r, MatchResult matchResult) {

    Map<String, Object> annoFeatures = annotation.getFeatures();
    Map<String, String> elementsFound = new HashMap<String, String>();
    int numGroups = matchResult.groupCount();
    for (int i = 0; i < numGroups + 1; i++) {
      String elemenValue = matchResult.group(i);
      String elemName = r.getElementMap().get(i);
      elementsFound.put(elemName, elemenValue);
      if (LOGGER.isDebugEnabled()) {
        annoFeatures.put(elemName, elemenValue);
      }
    }

    // the rule family tells us the set of elements to expect
    String family = r.getRuleFamily();
    String ruleName = r.getRuleName();

    if (DD_FAMILY.equals(family)) {

      Ordinate lat = null;
      Ordinate lon = null;
      try {
        lat = OrdinateParser.parse(elementsFound, AXIS.LATITUDE, OrdinateParser.ORDINATETYPE.DD);
        lon = OrdinateParser.parse(elementsFound, AXIS.LONGITUDE, OrdinateParser.ORDINATETYPE.DD);
      } catch (Exception e) {
        LOGGER.debug("Couldn't normalize " + annotation + " Ordinate Parser exception:", e);
      }

      if (lat != null && lon != null) {
        Geocoord geo = new Geocoord(lat.getOrdinateValue(), lon.getOrdinateValue());
        annoFeatures.put("geo", geo);
        annoFeatures.put("geoFamily", family);
        annoFeatures.put("geoPattern", ruleName);
      } else {
        annotation.setValid(false);
        LOGGER.debug("Couldn't normalize " + annotation);
      }

    }

    if (DM_FAMILY.equals(family) || DMS_FAMILY.equals(family)) {
      Ordinate lat = null;
      Ordinate lon = null;
      try {
        lat = OrdinateParser.parse(elementsFound, AXIS.LATITUDE, OrdinateParser.ORDINATETYPE.DMS);
        lon = OrdinateParser.parse(elementsFound, AXIS.LONGITUDE, OrdinateParser.ORDINATETYPE.DMS);
      } catch (Exception e) {
        LOGGER.debug("Couldn't normalize " + annotation + " Ordinate Parser exception:", e);
      }

      if (lat != null && lon != null) {
        Geocoord geo = new Geocoord(lat.getOrdinateValue(), lon.getOrdinateValue());
        annoFeatures.put("geo", geo);
        annoFeatures.put("geoFamily", family);
        annoFeatures.put("geoPattern", ruleName);
      } else {
        annotation.setValid(false);
        LOGGER.debug("Couldn't normalize " + annotation);
      }

    }

    if (MGRS_FAMILY.equals(family)) {

      List<MGRS> mgrsCandidates = null;
      try {
        mgrsCandidates = MGRSParser.parseMGRS(elementsFound);
      } catch (Exception e) {
        LOGGER.debug("Couldn't normalize " + annotation + " MGRS parser exception:", e);

      }

      if (mgrsCandidates != null && !mgrsCandidates.isEmpty()) {
        MGRS mgrs = mgrsCandidates.get(0);
        Geodetic2DPoint pt = mgrs.toGeodetic2DPoint();
        Geocoord geo = new Geocoord(pt.getLatitudeAsDegrees(), pt.getLongitudeAsDegrees());
        annoFeatures.put("geo", geo);
        annoFeatures.put("geoFamily", family);
        annoFeatures.put("geoPattern", ruleName);

        List<Geocoord> altCoords = new ArrayList<Geocoord>();
        if (mgrsCandidates.size() > 1) {
          for (MGRS m : mgrsCandidates) {
            Geodetic2DPoint pt2 = m.toGeodetic2DPoint();
            Geocoord geo2 = new Geocoord(pt2.getLatitudeAsDegrees(), pt2.getLongitudeAsDegrees());
            altCoords.add(geo2);
          }
          annoFeatures.put("geoAlternatives", altCoords);
        }
      } else {
        annotation.setValid(false);
        LOGGER.debug("Couldn't normalize " + annotation);
      }
    }
    if (UTM_FAMILY.equals(family)) {
      UTM utm = null;
      try {
        utm = UTMParser.parseUTM(elementsFound);
      } catch (Exception e) {
        LOGGER.debug("Couldn't normalize " + annotation + " UTM parser exception:", e);
      }

      if (utm != null) {
        Geodetic2DPoint pt = utm.getGeodetic();
        Geocoord geo = new Geocoord(pt.getLatitudeAsDegrees(), pt.getLongitudeAsDegrees());
        annoFeatures.put("geo", geo);
        annoFeatures.put("geoFamily", family);
        annoFeatures.put("geoPattern", ruleName);
      } else {
        annotation.setValid(false);
        LOGGER.debug("Couldn't normalize " + annotation);
      }
    }
    return;
  }
}
