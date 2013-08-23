/**
 *
 *  Copyright 2009-2013 The MITRE Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * **************************************************************************
 *                          NOTICE
 * This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 */
package org.opensextant.regex.geo;

import org.opensextant.regex.geo.OrdinateParser.AXIS;

public final class Ordinate {

  // Log object
  // private static Logger log = LoggerFactory.getLogger(Ordinate.class);

  // the ordinate type [DD,DMS]
  private OrdinateParser.ORDINATE_TYPE type = OrdinateParser.ORDINATE_TYPE.DD;

  // the magnitude of this ordinate [0.0 to LAT_MAX/LON_MAX,null]
  private Double ordinateValue = null;

  // hemisphere [-1,0,1]
  private int hemi = 0;

  // axis [LATITUDE,LONGITUDE,null]
  private OrdinateParser.AXIS axis = null;

  // the orginal text
  private String text = null;

  public Ordinate() {

  }

  public boolean isValid() {

    // check for elements being set to valid values
    if (ordinateValue == null || hemi == 0 || axis == null) {
      return false;
    }

    if (axis == AXIS.LATITUDE && ordinateValue > OrdinateParser.LAT_MAX) {
      return false;
    }

    if (axis == AXIS.LONGITUDE && ordinateValue > OrdinateParser.LON_MAX) {
      return false;
    }

    return true;

  }

  public Double getOrdinateValue() {

    if (this.ordinateValue == null || this.hemi == 0) {
      return null;
    }

    return hemi * ordinateValue;
  }

  public void setDegrees(Double degrees) {
    this.ordinateValue = degrees;

  }

  public void setMinutes(Double minutes) {
    if (this.ordinateValue == null) {
      this.ordinateValue = 0.0;
    }
    this.ordinateValue += (minutes / 60.0);
  }

  public void setSeconds(Double seconds) {
    if (this.ordinateValue == null) {
      this.ordinateValue = 0.0;
    }
    this.ordinateValue += (seconds / 3600.0);
  }

  public OrdinateParser.ORDINATE_TYPE getType() {
    return type;
  }

  public void setType(OrdinateParser.ORDINATE_TYPE type) {
    this.type = type;
  }

  public int getHemi() {
    return hemi;
  }

  public void setHemi(int hemi) {
    this.hemi = hemi;
  }

  public OrdinateParser.AXIS getAxis() {
    return axis;
  }

  public void setAxis(OrdinateParser.AXIS axis) {
    this.axis = axis;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

}
