/**
 Copyright 2009-2013 The MITRE Corporation.
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 * **************************************************************************
 *                          NOTICE
 * This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 **/
package org.opensextant.placedata;

import java.io.Serializable;

/**
 * A Place represents a named geographic location, a "place". It contains basic information about that place, such as
 * its name, its geographic location, the country it is part of or located in, what type of place it is (e.g. city,
 * river, province) and the original source of this information. These reflect the data traditionally found in
 * gazetteers.
 */
public class Place implements Comparable<Object>, Serializable {
  private static final long serialVersionUID = 2389068012345L;

  // Name metadata
  private String placeName;
  private String expandedPlaceName; // only present for abbrev/codes
  private String nameType;
  private String nameTypeSystem;

  // The geospatial data
  private String countryCode; // ISO2 code
  // private String countryCodeFIPS; // FIPS code
  // private String countryCodeISO3; // ISO3 code
  private String admin1;
  private String admin2;

  // what type of place this is
  private String featureClass;
  private String featureCode;

  // its location as a point
  private transient Geocoord geocoord = new Geocoord();

  // identifiers for this name and place
  private String sourceNameID;
  private String sourceFeatureID;
  private String placeID;
  // original source of this data
  private String source;

  // the a priori estimates
  private double nameBias;
  private double idBias;

  // values used for nameType
  public static final String ABBREV_TYPE = "abbrev";
  public static final String CODE_TYPE = "code";
  public static final String NAME_TYPE = "name";

  // construct an empty place
  public Place() {
  }

  // construct a place with just a name and ID
  public Place(String id, String name) {
    setPlaceID(id);
    setPlaceName(name);
  }

  @Override
  public String toString() {
    String output = "";
    if (this.expandedPlaceName != null) {
      output = this.placeName + " (" + this.expandedPlaceName + ")" + "(" + this.getAdmin1() + "," + this.countryCode
          + "," + this.featureCode + ")";
    } else {
      output = this.placeName + " (" + this.getAdmin1() + "," + this.countryCode + "," + this.featureCode + ")";
    }

    return output;
  }

  // two Places with the same PlaceID are the same "place"
  // two Places with different PlaceIDs ARE PROBABLY different "places"
  @Override
  public int compareTo(Object other) {
    if (!(other instanceof Place)) {
      return 0;
    }
    Place tmp = (Place) other;
    return this.placeID.compareTo(tmp.placeID);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Place)) {
      return false;
    }
    Place tmp = (Place) other;
    if (tmp.placeID.equalsIgnoreCase(tmp.placeID)) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * Is this Place a Country?
   * @return - true if this is a country or "country-like" place
   */
  public boolean isACountry() {
    return featureCode.startsWith("PCL");
  }

  /**
   * Is this Place a State or Province?
   * @return - true if this is a State, Province or other first level admin area
   */
  public boolean isAnAdmin1() {
    return "ADM1".equalsIgnoreCase(featureCode);
  }

  /**
   * Is this Place a National Capital?
   * @return - true if this is a a national Capital area
   */
  public boolean isNationalCapital() {
    return "PPLC".equalsIgnoreCase(featureCode);
  }

  /**
   * Is this name an abbreviation or code?
   * @return - true if this name is an abbreviation or code
   */
  public boolean isAbbreviation() {

    if (ABBREV_TYPE.equalsIgnoreCase(this.nameType)) {
      return true;
    }

    if (CODE_TYPE.equalsIgnoreCase(this.nameType)) {
      return true;
    }

    return false;
  }

  // The getters and setters
  public String getPlaceName() {
    return placeName;
  }

  public void setPlaceName(String placeName) {
    this.placeName = placeName;
  }

  public String getExpandedPlaceName() {
    return expandedPlaceName;
  }

  public void setExpandedPlaceName(String expandedPlaceName) {
    this.expandedPlaceName = expandedPlaceName;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public String getFeatureClass() {
    return featureClass;
  }

  public void setFeatureClass(String featureClass) {
    this.featureClass = featureClass;
  }

  public String getFeatureCode() {
    return featureCode;
  }

  public void setFeatureCode(String featureCode) {
    this.featureCode = featureCode;
  }

  public Double getLatitude() {
    return this.geocoord.getLatitude();
  }

  public void setLatitude(Double latitude) {
    this.geocoord.setLatitude(latitude);
  }

  public Double getLongitude() {
    return this.geocoord.getLongitude();
  }

  public void setLongitude(Double longitude) {
    this.geocoord.setLongitude(longitude);
  }

  public String getSourceNameID() {
    return sourceNameID;
  }

  public void setSourceNameID(String uni) {
    this.sourceNameID = uni;
  }

  public String getSourceFeatureID() {
    return sourceFeatureID;
  }

  public void setSourceFeatureID(String ufi) {
    this.sourceFeatureID = ufi;
  }

  public String getPlaceID() {
    return placeID;
  }

  public void setPlaceID(String placeID) {
    this.placeID = placeID;
  }

  public String getAdmin1() {
    return admin1;
  }

  public void setAdmin1(String key) {
    this.admin1 = key;
  }

  public String getAdmin2() {
    return admin2;
  }

  public void setAdmin2(String key) {
    this.admin2 = key;
  }

  /**
   * Get the original source of this information.
   */
  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getNameType() {
    return nameType;
  }

  public void setNameType(String nameType) {
    this.nameType = nameType;
  }

  /**
   * The name bias is a measure of the a priori likelihood that a mention of this place's name actually refers to a
   * place.
   */
  public double getNameBias() {
    return nameBias;
  }

  public void setNameBias(double nameBias) {
    this.nameBias = nameBias;
  }

  /**
   * The ID bias is a measure of the a priori likelihood that a mention of this name refers to this particular place.
   */
  public double getIdBias() {
    return idBias;
  }

  public void setIdBias(double idBias) {
    this.idBias = idBias;
  }

  public Geocoord getGeocoord() {
    return geocoord;
  }

  public void setGeocoord(Geocoord geocoord) {
    this.geocoord = geocoord;
  }

  public String getNameTypeSystem() {
    return nameTypeSystem;
  }

  public void setNameTypeSystem(String nameTypeSystem) {
    this.nameTypeSystem = nameTypeSystem;
  }

}
