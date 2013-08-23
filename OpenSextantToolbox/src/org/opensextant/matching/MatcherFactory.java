package org.opensextant.matching;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.DateUtil;
import org.apache.solr.core.CoreContainer;
import org.opensextant.placedata.Place;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatcherFactory {

  private MatcherFactory() {
  }

  // the environment variable for the default location
  private static String envParam = "solr.home";

  // string specifying solr home, could be file path or URL
  private static String homeLocation;

  // states of solr server and thus the MatcherFactory
  private static boolean isRemote = false;
  private static boolean isConfigured = false;
  private static boolean isStarted = false;

  // the solr server which is the heart of the MatcherFactory
  private static SolrServer solrServer = null;

  // all of the Matchers and Searchers the Factory has created
  // weak references so they can be GC'ed
  static Map<PlacenameMatcher, Boolean> matchers = new WeakHashMap<PlacenameMatcher, Boolean>();
  static Map<PlacenameSearcher, Boolean> searchers = new WeakHashMap<PlacenameSearcher, Boolean>();

  // the fields of the match and query response
  private static String fieldnames = "id,place_id,name,lat,lon,geo,feat_class,feat_code," +
    "FIPS_cc,cc,ISO3_cc,adm1,adm2,adm3,adm4,adm5,source,src_place_id,src_name_id,script," +
    "conflate_key,name_bias,id_bias,name_type,name_type_system,partition";

  // the initial parameters for matchers and the searchers
  private static ModifiableSolrParams matchParams = new ModifiableSolrParams();
  private static ModifiableSolrParams searchParams = new ModifiableSolrParams();

  // the matching request handler
  private static final String MATCH_REQUESTHANDLER = "/tag";

  // Log object
  private static Logger log = LoggerFactory.getLogger(MatcherFactory.class);

  // set the base config for the matching and searching params
  static {
    matchParams.set(CommonParams.QT, MATCH_REQUESTHANDLER);
    matchParams.set(CommonParams.FL, fieldnames);
    matchParams.set("tagsLimit", 100000);
    matchParams.set(CommonParams.ROWS, 100000);
    matchParams.set("subTags", false);
    matchParams.set("matchText", false);
    matchParams.set("overlaps", "LONGEST_DOMINANT_RIGHT");
    matchParams.set("field", "name4matching");

    searchParams.set(CommonParams.Q, "*:*");
    searchParams.set(CommonParams.FL, fieldnames + ",score");
  }

  /**
   * Configure this MatcherFctory
   * @param home
   *          solr home as a file path or URL
   */
  public static void config(String home) {

    if (isStarted) {
      // already running
      return;
    }

    // not running but already configured must be re-configuring
    if (isConfigured) {
      isRemote = false;
      isConfigured = false;
      isStarted = false;
      solrServer = null;
    }

    // no home given default to env param
    if (home == null || home.length() == 0) {
      String solrEnv = System.getenv(envParam);
      if (solrEnv != null && solrEnv.length() > 0) {
        if (validFile(solrEnv)) {
          isRemote = false;
          homeLocation = solrEnv;
          isConfigured = true;
          return;
        }
      } else {
        // nothing given and nothing in env
        isConfigured = false;
        return;
      }
    }

    // remote solr
    if (home.toLowerCase().startsWith("http:") && validURL(home)) {
      isConfigured = true;
      isRemote = true;
      homeLocation = home;
      isConfigured = true;
      return;
    }

    // local using file URL
    if (home.toLowerCase().startsWith("file:") && validURL(home)) {

      URL tmpURL = null;
      try {
        tmpURL = new URL(home);
      } catch (MalformedURLException e) {
        // how could this happen? we just checked it was valid
        log.error("Malformed URL in initializing MatcherFactory:" + tmpURL, e);
        return;
      }

      String filePath = tmpURL.getPath();
      if (validFile(filePath)) {
        homeLocation = filePath;
      } else {
        return;
      }

      isRemote = false;
      isConfigured = true;
      return;
    }

    // anything else, local using file path
    if (validFile(home)) {
      isRemote = false;
      homeLocation = home;
      isConfigured = true;
      return;
    }

    // fell through no valid config
    homeLocation = "";
    isConfigured = false;
    log.error("Could not configure the MatcherFactory using " + home);
    return;

  }

  /**
   * Start this MatcherFactory
   */
  public static void start() {

    if (!isConfigured) {
      // can't start not configured
      log.error("Could not start MatcherFactory, it hasnt been configured yet");
      return;
    }

    // already started
    if (isStarted) {
      log.info("Tried to start MatcherFactory when it was already started");
      return;
    }

    if (isRemote) {
      HttpSolrServer server = new HttpSolrServer(homeLocation);
      server.setAllowCompression(true);
      solrServer = server;
    } else {
      try {
        File solrXML = new File(homeLocation + File.separator + "solr.xml");
        CoreContainer solrContainer = new CoreContainer(homeLocation);
        solrContainer.load(homeLocation, solrXML);
        EmbeddedSolrServer server = new EmbeddedSolrServer(solrContainer, "");
        solrServer = server;
      } catch (FileNotFoundException e) {
        // this should never happen since we check before calling
        log.error("Could not find solr home when initializing MatcherFactory:" + homeLocation, e);
        return;
      }
    }

    // see if it is really there
    SolrPingResponse ping;
    try {
      ping = solrServer.ping();
    } catch (SolrServerException e) {
      log.error("Solr Server didn't respond to ping from MatcherFactory", e);
      isStarted = false;
      return;
    } catch (IOException e) {
      log.error("Solr Server didn't respond to ping from MatcherFactory", e);
      isStarted = false;
      return;
    }

    // started and got good ping
    if (ping.getStatus() == 0) {
      isStarted = true;
    } else {
      log.error("Solr Server responded with error code from ping from MatcherFactory. Got code:" + ping.getStatus());
      isStarted = false;
    }
    // do warmup here?
    return;

  }

  /**
   * Get a PlacenameMatcher
   * @return a PlacenameMatcher
   */
  public static PlacenameMatcher getMatcher() {

    // if started/configed etc
    if (isConfigured) {

      if (isStarted) {
        PlacenameMatcher tmp = new PlacenameMatcher(solrServer, matchParams);
        matchers.put(tmp, true);
        return tmp;
      } else {
        // configured but not started
        start();
        log.debug("Autostarting MatcherFactory");
        PlacenameMatcher tmp = new PlacenameMatcher(solrServer, matchParams);
        matchers.put(tmp, true);
        return tmp;
      }
    } else {
      // not configured
      // try default config
      log.debug("Trying default config and autostarting Matcher Factory");
      config("");
      if (isConfigured) {
        log.debug("Default config worked. Try to start");
        start();
        PlacenameMatcher tmp = new PlacenameMatcher(solrServer, matchParams);
        matchers.put(tmp, true);
        return tmp;
      } else {
        log.error("MatcherFactory not configured and default config did'nt work");
        return null;
      }
    }
  }

  /**
   * Get a PlacenameSearcher
   * @return a PlacenameSearcher
   */
  public static PlacenameSearcher getSearcher() {

    // if started/configed etc
    if (isConfigured) {

      if (isStarted) {
        PlacenameSearcher tmp = new PlacenameSearcher(solrServer, searchParams);
        searchers.put(tmp, true);
        return tmp;
      } else {
        // configured but not started
        start();
        log.debug("Autostarting MatcherFactory");
        PlacenameSearcher tmp = new PlacenameSearcher(solrServer, searchParams);
        searchers.put(tmp, true);
        return tmp;
      }
    } else {
      // not configured
      // try default config
      log.debug("Trying default config and autostarting Matcher Factory");
      config("");
      if (isConfigured) {
        log.debug("Default config worked. Try to start");
        start();
        PlacenameSearcher tmp = new PlacenameSearcher(solrServer, searchParams);
        searchers.put(tmp, true);
        return tmp;
      } else {
        log.error("MatcherFactory not configured and default config did'nt work");
        return null;
      }

    }

  }

  /**
   * @param mtcher
   *          the matcher which is requesting the shutdown
   */
  static void shutdown(PlacenameMatcher mtcher) {
    matchers.remove(mtcher);
    MatcherFactory.shutdown(false);
  }

  /**
   * Request a shutdown
   * @param srcher
   *          the searcher which is request the shutdown
   */
  static void shutdown(PlacenameSearcher srcher) {
    searchers.remove(srcher);
    MatcherFactory.shutdown(false);
  }

  /**
   * Shutdown the MatcherFactory
   * @param force
   *          if true, force a shutdown even if there are matchers and searchers still out there (rude)
   */
  public static void shutdown(boolean force) {

    if (force) {
      if (solrServer != null) {
        solrServer.shutdown();
      }
      isStarted = false;
    } else {
      if (solrServer != null && !factoryInUse()) {
        solrServer.shutdown();
        isStarted = false;
      }
    }
  }

  private static boolean factoryInUse() {
    return !matchers.isEmpty() || !searchers.isEmpty();
  }

  /**
   * Check if a URL is valid
   * @param url
   *          the URL to check
   * @return
   */
  private static boolean validURL(String url) {

    URL solrURL = null;

    try {
      solrURL = new URL(url);
    } catch (MalformedURLException e) {
      // eat the exception and return not valid
      return false;
    }

    // just so FindBug doesn't complain about unused variable
    solrURL.getHost();
    // check for existence/access here?
    // solrURL.openStream(); ?

    return true;

  }

  private static boolean validFile(String file) {

    File tmp = new File(file);

    if (!tmp.exists()) {
      // file doesn't exist
      return false;
    }

    if (!tmp.isDirectory()) {
      // not a directory
      return false;
    }

    File solrXML = new File(tmp, "solr.xml");

    if (!solrXML.exists()) {
      // doesn't contain a solr.xml
      return false;
    }

    return true;

  }

  public static boolean isConfigured() {
    return isConfigured;
  }

  public static boolean isStarted() {
    return isStarted;
  }

  public static String getHomeLocation() {
    return homeLocation;
  }

  /**
   * Get an integer from a record
   */
  public static int getInteger(SolrDocument d, String f) {
    Object obj = d.getFieldValue(f);
    if (obj == null) {
      return 0;
    }
    if (obj instanceof Integer) {
      return ((Integer) obj).intValue();
    } else {
      Integer v = Integer.parseInt(obj.toString());
      return v.intValue();
    }
  }

  /**
   * Get a floating point object from a record
   */
  public static Float getFloat(SolrDocument d, String f) {
    Object obj = d.getFieldValue(f);
    if (obj == null) {
      return 0F;
    } else {
      return (Float) obj;
    }
  }

  /**
   * Get a Date object from a record
   * @throws java.text.ParseException
   */
  public static Date getDate(SolrDocument d, String f) throws java.text.ParseException {
    if (d == null || f == null) {
      return null;
    }
    Object obj = d.getFieldValue(f);
    if (obj == null) {
      return null;
    }
    if (obj instanceof Date) {
      return (Date) obj;
    } else if (obj instanceof String) {
      return DateUtil.parseDate((String) obj);
    }
    return null;
  }

  /**
     *
     */
  public static char getChar(SolrDocument solrDoc, String name) {
    String result = getString(solrDoc, name);
    if (result == null) {
      return 0;
    }
    if (result.isEmpty()) {
      return 0;
    }
    return result.charAt(0);
  }

  /**
   * Get a String object from a record
   */
  public static String getString(SolrDocument solrDoc, String name) {
    Object result = solrDoc.getFirstValue(name);
    if (result == null) {
      return null;
    }
    return result.toString();
  }

  /**
   * Get a double from a record
   */
  public static double getDouble(SolrDocument solrDoc, String name) {
    Object result = solrDoc.getFirstValue(name);
    if (result == null) {
      throw new IllegalStateException("Blank: " + name + " in " + solrDoc);
    }
    if (result instanceof Number) {
      Number number = (Number) result;
      return number.doubleValue();
    } else {
      return Double.parseDouble(result.toString());
    }
  }

  /**
   * Parse XY pair stored in Solr Spatial4J record. No validation is done.
   * @return XY double array, [lat, lon]
   */
  public static double[] getCoordinate(SolrDocument solrDoc, String field) {
    String xy = (String) solrDoc.getFirstValue(field);
    if (xy == null) {
      throw new IllegalStateException("Blank: " + field + " in " + solrDoc);
    }
    final double[] xyPair = {0.0, 0.0};
    String[] latLon = xy.split(",", 2);
    xyPair[0] = Double.parseDouble(latLon[0]);
    xyPair[1] = Double.parseDouble(latLon[1]);
    return xyPair;
  }

  /**
   * Parse XY pair stored in Solr Spatial4J record. No validation is done.
   * @return XY double array, [lat, lon]
   */
  public static double[] getCoordinate(String xy) {
    final double[] xyPair = {0.0, 0.0};
    String[] latLon = xy.split(",", 2);
    xyPair[0] = Double.parseDouble(latLon[0]);
    xyPair[1] = Double.parseDouble(latLon[1]);
    return xyPair;
  }

  /**
   * @param in
   *          a string to be interned
   * @return the interned string
   */
  public static String internString(String in) {
    if (in != null) {
      in = in.intern();
    }
    return in;
  }

  /**
   * Create a Place object from a Solr document
   * @param gazEntry
   *          a solr document describing a Place
   * @return the Place created from the solr Document
   */
  public static Place createPlace(SolrDocument gazEntry) {

    // create the basic Place
    Place place = new Place(getString(gazEntry, "place_id"), getString(gazEntry, "name"));

    // set name type and nameTypeSystem
    place.setNameType(internString(getString(gazEntry, "name_type")));
    place.setNameTypeSystem(internString(getString(gazEntry, "name_type_system")));

    // set country coude using the cc (ISO2) value
    place.setCountryCode(internString(getString(gazEntry, "cc")));

    // Set the admin values
    place.setAdmin1(internString(getString(gazEntry, "adm1")));
    place.setAdmin2(internString(getString(gazEntry, "adm2")));

    // set the feature class and code
    place.setFeatureClass(internString(getString(gazEntry, "feat_class")));
    place.setFeatureCode(internString(getString(gazEntry, "feat_code")));

    // set the source
    place.setSource(internString(getString(gazEntry, "source")));

    // set the geo
    double[] xy = getCoordinate(gazEntry, "geo");
    place.setLatitude(xy[0]);
    place.setLongitude(xy[1]);

    // set the bias values
    place.setNameBias(getDouble(gazEntry, "name_bias"));
    place.setIdBias(getDouble(gazEntry, "id_bias"));

    return place;
  }

}
