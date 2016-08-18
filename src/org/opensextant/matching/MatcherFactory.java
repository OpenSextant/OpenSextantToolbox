package org.opensextant.matching;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.DateUtil;
import org.apache.solr.core.CoreContainer;
import org.opensextant.placedata.Place;
import org.opensextant.vocab.Vocab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatcherFactory {

	/** The name of environment var and system property for solr. */
	private static String envParam = "solr.home";

	/** String specifying solr home, could be file path or URL. */
	private static String homeLocation;

	/**
	 * States of solr server and thus the MatcherFactory is solr accessed via
	 * URL (remote) or embedded?
	 */
	private static boolean isRemote;
	/** Do we have a valid solr home? */
	private static boolean isConfigured;
	/** Have we started solr? */
	private static boolean isStarted;

	/** The solr servers which are the heart of the MatcherFactory. */
	private static SolrClient solrServerGeo;
	private static SolrClient solrServerVocab;

	/**
	 * All of the Matchers,Searchers and VocabMatchers the Factory has created
	 * weak references so they can be GC'ed.
	 */
	static Map<PlacenameMatcher, Boolean> matchers = new WeakHashMap<PlacenameMatcher, Boolean>();
	static Map<PlacenameSearcher, Boolean> searchers = new WeakHashMap<PlacenameSearcher, Boolean>();
	static Map<VocabMatcher, Boolean> vocabers = new WeakHashMap<VocabMatcher, Boolean>();

	/** The fields of the geo match and query response. */
	private static String gazetteerFieldNames = "id,place_id,name,name_expanded,lat,lon,geo,feat_class,feat_code,"
			+ "FIPS_cc,cc,ISO3_cc,adm1,adm2,adm3,adm4,adm5,source,src_place_id,src_name_id,script,"
			+ "name_bias,id_bias,name_type,name_type_system,partition,search_only";

	/**
	 * The field names to load the gazetteer (same as match/query except for
	 * "geo" field which is created on load.
	 */
	private static String gazetteerFieldNamesLoader = "id,place_id,name,name_expanded,lat,lon,feat_class,feat_code,"
			+ "FIPS_cc,cc,ISO3_cc,adm1,adm2,adm3,adm4,adm5,source,src_place_id,src_name_id,script,"
			+ "name_bias,id_bias,name_type,name_type_system,partition,search_only";

	/** The fixed fields of the vocab match and response. */
	private static String vocabFieldNames = "id,phrase,category,taxonomy";

	/** The initial parameters for matchers and the searchers and vocabers. */
	private static ModifiableSolrParams matchParams = new ModifiableSolrParams();
	private static ModifiableSolrParams searchParams = new ModifiableSolrParams();
	private static ModifiableSolrParams vocabParams = new ModifiableSolrParams();

	/**
	 * Mapping from gazetteer codes to hierachical expression used on the Place
	 * object.
	 */
	private static Map<String, String> featureCodeMap = new HashMap<String, String>();

	/** The matching request handler. */
	private static final String MATCH_REQUESTHANDLER = "/tag";

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MatcherFactory.class);

	/** Set the base config for the matching and searching params. */
	static {
		matchParams.set(CommonParams.QT, MATCH_REQUESTHANDLER);
		matchParams.set(CommonParams.FL, gazetteerFieldNames);
		matchParams.set("tagsLimit", 100000);
		matchParams.set(CommonParams.ROWS, 100000);
		matchParams.set("subTags", false);
		matchParams.set("matchText", false);
		matchParams.set("overlaps", "LONGEST_DOMINANT_RIGHT");
		matchParams.set("field", "name4matching");
		matchParams.set(CommonParams.FQ, "search_only:false");

		searchParams.set(CommonParams.Q, "*:*");
		searchParams.set(CommonParams.FL, gazetteerFieldNames + ",score");
		searchParams.set(CommonParams.ROWS, 100000);

		vocabParams.set(CommonParams.QT, MATCH_REQUESTHANDLER);
		vocabParams.set(CommonParams.FL, vocabFieldNames);
		vocabParams.set("tagsLimit", 100000);
		vocabParams.set(CommonParams.ROWS, 100000);
		vocabParams.set("subTags", false);
		vocabParams.set("matchText", false);
		vocabParams.set("overlaps", "LONGEST_DOMINANT_RIGHT");
		vocabParams.set("field", "phrase4matching");

	}

	/** Set the values of the feature codes. */

	static {
		featureCodeMap.put("A", "Geo.featureType.AdminRegion");
		featureCodeMap.put("P", "Geo.featureType.PopulatedPlace");
		featureCodeMap.put("V", "Geo.featureType.Vegetation");
		featureCodeMap.put("L", "Geo.featureType.Area");
		featureCodeMap.put("U", "Geo.featureType.Undersea");
		featureCodeMap.put("R", "Geo.featureType.Street");
		featureCodeMap.put("T", "Geo.featureType.Hypso");
		featureCodeMap.put("H", "Geo.featureType.Hydro");
		featureCodeMap.put("S", "Geo.featureType.SpotFeature");
	}

	private MatcherFactory() {
	}

	/**
	 * Configure this MatcherFctory.
	 * 
	 * @param home
	 *            solr home as a file path or URL
	 */
	public static void config(String home) {

		if (isStarted) {
			// already running
			LOGGER.info("Tried to configure MatcherFactory when already started. Doing nothing.");
			return;
		}

		// not running but already configured, must be re-configuring
		if (isConfigured) {
			LOGGER.info("Trying to re-configure MatcherFactory.");
			isRemote = false;
			isConfigured = false;
			isStarted = false;
			solrServerGeo = null;
			solrServerVocab = null;
		}

		// get value for home
		boolean foundHome = setHome(home);
		if (!foundHome) {
			isConfigured = false;
			LOGGER.error("Could not configure MatcherFactory: Could not find a value for solr home");
			return;
		}

		// determine if home is local or remote
		boolean foundRemoteLocal = setLocalorRemote();
		if (!foundRemoteLocal) {
			isConfigured = false;
			LOGGER.error("Could not configure MatcherFactory: Could not interpret:" + homeLocation);
			return;
		}

		// all OK
		isConfigured = true;
		LOGGER.info("Configured MatcherFactory: solr.home=" + homeLocation + " Remote=" + isRemote);
	}

	/**
	 * Start this MatcherFactory.
	 */
	public static void start() {

		if (!isConfigured) {
			// can't start not configured
			LOGGER.error("Could not start MatcherFactory, it hasn't been configured yet");
			return;
		}

		// already started
		if (isStarted) {
			LOGGER.info("Tried to start MatcherFactory when it was already started. Doing nothing.");
			return;
		}

		// if remote, use HttpSolrServer
		if (isRemote) {
			HttpSolrClient server = new HttpSolrClient(homeLocation+"gazetteer");
			server.setAllowCompression(true);
			solrServerGeo = server;
			server = new HttpSolrClient(homeLocation+"vocabulary");
			server.setAllowCompression(true);
			solrServerVocab = server;
		} else { // must be local, use EmbeddedSolrServer

			CoreContainer solrContainer = new CoreContainer(homeLocation);

			solrContainer.load();
			EmbeddedSolrServer serverGeo = new EmbeddedSolrServer(solrContainer, "gazetteer");
			EmbeddedSolrServer serverVocab = new EmbeddedSolrServer(solrContainer, "vocabulary");
			solrServerGeo = serverGeo;
			solrServerVocab = serverVocab;
		}

		// see if solr servers are really there
		SolrPingResponse pingGeo;
		SolrPingResponse pingVocab;
		try {
			pingGeo = solrServerGeo.ping();
			pingVocab = solrServerVocab.ping();
		} catch (SolrServerException e) {
			LOGGER.error("Solr Server didn't respond to ping from MatcherFactory", e);
			isStarted = false;
			return;
		} catch (IOException e) {
			LOGGER.error("Solr Server didn't respond to ping from MatcherFactory", e);
			isStarted = false;
			return;
		}

		// started and got good ping
		if (pingGeo.getStatus() == 0) {
			isStarted = true;
		} else {
			LOGGER.error("Solr Server (Geo) responded with error code from ping from MatcherFactory. Got code:"
					+ pingGeo.getStatus());
			isStarted = false;
		}

		// started and got good ping
		if (pingVocab.getStatus() == 0) {
			isStarted = true;
		} else {
			LOGGER.error("Solr Server (Vocab) responded with error code from ping from MatcherFactory. Got code:"
					+ pingVocab.getStatus());
			isStarted = false;
		}

		// do warmup here?
		return;

	}

	/** Set the value for solr home. */
	private static boolean setHome(String home) {

		String foundIt = home;
		// explicit value given?
		if (foundIt != null && foundIt.length() > 0) {
			homeLocation = foundIt;
			LOGGER.info("Explicit solr home found. Using " + foundIt);
			return true;
		} else {
			LOGGER.debug("No explicit value given for solr home. Checking for system property");
		}

		// system property?
		foundIt = System.getProperty(envParam);
		if (foundIt != null && foundIt.length() > 0) {
			homeLocation = foundIt;
			LOGGER.info("System property for solr home found. Using " + foundIt);
			return true;
		} else {
			LOGGER.debug("No " + envParam + " system property for solr home. Checking for env variable");
		}

		// environment variable?
		foundIt = System.getenv(envParam);
		if (foundIt != null && foundIt.length() > 0) {
			homeLocation = foundIt;
			LOGGER.info("Environment variable for solr home found. Using " + foundIt);
			return true;
		} else {
			LOGGER.debug("No " + envParam + " environment variable for solr home");
		}

		// TODO add some sort of default location?

		LOGGER.error("Tried everything and no value for solr home found");

		return false;

	}

	private static boolean setLocalorRemote() {

		if (validRemoteURL(homeLocation)) {
			isRemote = true;
			return true;
		}

		if (validFileURL(homeLocation)) {
			isRemote = false;
			return true;
		}

		if (validFile(homeLocation)) {
			isRemote = false;
			return true;
		}

		return false;

	}

	/**
	 * Get a PlacenameMatcher.
	 * 
	 * @return a PlacenameMatcher
	 */
	public static PlacenameMatcher getMatcher() {

		// if started/configed etc
		if (isConfigured) {

			if (isStarted) {
				PlacenameMatcher tmp = new PlacenameMatcher(solrServerGeo, matchParams);
				matchers.put(tmp, true);
				return tmp;
			} else {
				// configured but not started
				start();
				LOGGER.debug("Autostarting MatcherFactory");
				PlacenameMatcher tmp = new PlacenameMatcher(solrServerGeo, matchParams);
				matchers.put(tmp, true);
				return tmp;
			}
		} else {
			// not configured
			// try default config
			LOGGER.debug("Trying default config and autostarting Matcher Factory");
			config("");
			if (isConfigured) {
				LOGGER.debug("Default config worked. Try to start");
				start();
				PlacenameMatcher tmp = new PlacenameMatcher(solrServerGeo, matchParams);
				matchers.put(tmp, true);
				return tmp;
			} else {
				LOGGER.error("MatcherFactory not configured and default config didn't work");
				return null;
			}
		}
	}

	/**
	 * Get a PlacenameSearcher.
	 * 
	 * @return a PlacenameSearcher
	 */
	public static PlacenameSearcher getSearcher() {

		// if started/configed etc
		if (isConfigured) {

			if (isStarted) {
				PlacenameSearcher tmp = new PlacenameSearcher(solrServerGeo, searchParams);
				searchers.put(tmp, true);
				return tmp;
			} else {
				// configured but not started
				start();
				LOGGER.debug("Autostarting MatcherFactory");
				PlacenameSearcher tmp = new PlacenameSearcher(solrServerGeo, searchParams);
				searchers.put(tmp, true);
				return tmp;
			}
		} else {
			// not configured
			// try default config
			LOGGER.debug("Trying default config and autostarting Matcher Factory");
			config("");
			if (isConfigured) {
				LOGGER.debug("Default config worked. Try to start");
				start();
				PlacenameSearcher tmp = new PlacenameSearcher(solrServerGeo, searchParams);
				searchers.put(tmp, true);
				return tmp;
			} else {
				LOGGER.error("MatcherFactory not configured and default config did'nt work");
				return null;
			}

		}

	}

	/**
	 * Get a VocabMatcher.
	 * 
	 * @return a VocabMatcher
	 */
	public static VocabMatcher getVocabMatcher() {

		// if started/configed etc
		if (isConfigured) {

			if (isStarted) {
				VocabMatcher tmp = new VocabMatcher(solrServerVocab, vocabParams);
				vocabers.put(tmp, true);
				return tmp;
			} else {
				// configured but not started
				start();
				LOGGER.debug("Autostarting MatcherFactory");
				VocabMatcher tmp = new VocabMatcher(solrServerVocab, vocabParams);
				vocabers.put(tmp, true);
				return tmp;
			}
		} else {
			// not configured
			// try default config
			LOGGER.debug("Trying default config and autostarting Matcher Factory");
			config("");
			if (isConfigured) {
				LOGGER.debug("Default config worked. Try to start");
				start();
				VocabMatcher tmp = new VocabMatcher(solrServerVocab, vocabParams);
				vocabers.put(tmp, true);
				return tmp;
			} else {
				LOGGER.error("MatcherFactory not configured and default config did'nt work");
				return null;
			}

		}

	}

	/**
	 * @param mtcher
	 *            the matcher which is requesting the shutdown
	 */
	protected static void shutdown(PlacenameMatcher mtcher) {
		matchers.remove(mtcher);
		MatcherFactory.shutdown(false);
	}

	/**
	 * Request a shutdown.
	 * 
	 * @param srcher
	 *            the searcher which is request the shutdown
	 */
	protected static void shutdown(PlacenameSearcher srcher) {
		searchers.remove(srcher);
		MatcherFactory.shutdown(false);
	}

	protected static void shutdown(VocabMatcher vocabMatcher) {
		vocabers.remove(vocabMatcher);
		MatcherFactory.shutdown(false);
	}

	/**
	 * Shutdown the MatcherFactory.
	 * 
	 * @param force
	 *            if true, force a shutdown even if there are matchers and
	 *            searchers still out there (rude)
	 */
	public static void shutdown(boolean force) {

		if (force) {
			try {
				if (solrServerGeo != null) {
					solrServerGeo.close();
				}
				if (solrServerVocab != null) {
					solrServerVocab.close();
				}
			} catch (IOException e) {
				LOGGER.error("Error trying close MatcherFactory" + e.getMessage());
			}
			isStarted = false;
		} else {
			try {
				if (solrServerGeo != null && !factoryInUse()) {
					solrServerGeo.close();
					isStarted = false;
				}
				if (solrServerVocab != null && !factoryInUse()) {
					solrServerVocab.close();
					isStarted = false;
				}
			} catch (IOException e) {
				LOGGER.error("Error trying close MatcherFactory" + e.getMessage());
			}

		}
	}

	private static boolean factoryInUse() {
		return !matchers.isEmpty() || !searchers.isEmpty();
	}

	/**
	 * Check if a URL is valid.
	 * 
	 * @param url
	 *            the URL to check
	 * @return
	 */
	private static boolean validRemoteURL(String url) {

		URL solrURL = null;

		try {
			solrURL = new URL(url);
		} catch (MalformedURLException e) {
			// eat the exception and return not valid
			return false;
		}

		return "http".equalsIgnoreCase(solrURL.getProtocol());
	}

	private static boolean validFileURL(String url) {

		URL solrURL = null;

		try {
			solrURL = new URL(url);
		} catch (MalformedURLException e) {
			// eat the exception and return not valid
			return false;
		}

		// some sort of URL, check to see if its is a file URL
		if ("file".equalsIgnoreCase(solrURL.getProtocol())) {

			// see if points to something
			File tmp;
			try {
				tmp = new File(solrURL.toURI());
			} catch (URISyntaxException e) {
				// can't convert to file.
				LOGGER.error("Cannot use " + url + " as solr home. Doesn't appear to be valid file URL", e);
				return false;
			}

			// check if valid file
			return validFile(tmp.getAbsolutePath());
		}

		return false;
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

		return solrXML.exists();

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

	protected static String getGazetteerFieldNames() {
		return gazetteerFieldNames;
	}

	protected static String getGazetteerFieldNamesLoader() {
		return gazetteerFieldNamesLoader;
	}

	protected static String getVocabFieldNames() {
		return vocabFieldNames;
	}

	protected static SolrClient getSolrServerGeo() {
		return solrServerGeo;
	}

	protected static SolrClient getSolrServerVocab() {
		return solrServerVocab;
	}

	/**
	 * Get an integer from a record.
	 */
	protected static int getInteger(SolrDocument d, String f) {
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
	 * Get a floating point object from a record.
	 */
	protected static Float getFloat(SolrDocument d, String f) {
		Object obj = d.getFieldValue(f);
		if (obj != null) {
			return (Float) obj;
		} else {
			return 0F;
		}
	}

	/**
	 * Get a Date object from a record
	 * 
	 * @throws java.text.ParseException
	 */
	protected static Date getDate(SolrDocument d, String f) throws java.text.ParseException {
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

	protected static char getChar(SolrDocument solrDoc, String name) {
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
	 * Get a String object from a record.
	 */
	protected static String getString(SolrDocument solrDoc, String name) {
		Object result = solrDoc.getFirstValue(name);
		if (result != null) {
			return result.toString();
		}
		return null;
	}

	/**
	 * Get a double from a record.
	 */
	protected static double getDouble(SolrDocument solrDoc, String name) {
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
	 * 
	 * @return XY double array, [lat, lon]
	 */
	protected static double[] getCoordinate(SolrDocument solrDoc, String field) {
		String xy = (String) solrDoc.getFirstValue(field);
		if (xy == null) {
			throw new IllegalStateException("Blank: " + field + " in " + solrDoc);
		}
		final double[] xyPair = { 0.0, 0.0 };
		String[] latLon = xy.split(",", 2);
		xyPair[0] = Double.parseDouble(latLon[0]);
		xyPair[1] = Double.parseDouble(latLon[1]);
		return xyPair;
	}

	/**
	 * Parse XY pair stored in Solr Spatial4J record. No validation is done.
	 * 
	 * @return XY double array, [lat, lon]
	 */
	protected static double[] getCoordinate(String xy) {
		final double[] xyPair = { 0.0, 0.0 };
		String[] latLon = xy.split(",", 2);
		xyPair[0] = Double.parseDouble(latLon[0]);
		xyPair[1] = Double.parseDouble(latLon[1]);
		return xyPair;
	}

	/**
	 * @param in
	 *            a string to be interned
	 * @return the interned string
	 */
	protected static String internString(String in) {
		if (in != null) {
			return in.intern();
		}
		return in;
	}

	/**
	 * Create a Place object from a Solr document.
	 * 
	 * @param gazEntry
	 *            a solr document describing a Place
	 * @return the Place created from the solr Document
	 */
	protected static Place createPlace(SolrDocument gazEntry) {

		// create the basic Place
		Place place = new Place(getString(gazEntry, "place_id"), getString(gazEntry, "name"));

		// add the expanded name
		place.setExpandedPlaceName(getString(gazEntry, "name_expanded"));

		// set name type and nameTypeSystem
		place.setNameType(internString(getString(gazEntry, "name_type")));
		place.setNameTypeSystem(internString(getString(gazEntry, "name_type_system")));

		// set country coude using the cc (ISO2) value
		place.setCountryCode(internString(getString(gazEntry, "cc")));

		// Set the admin values
		place.setAdmin1(internString(getString(gazEntry, "adm1")));
		place.setAdmin2(internString(getString(gazEntry, "adm2")));

		// map and set the feature class and code
		place.setFeatureClass(internString(featureCodeMap.get(getString(gazEntry, "feat_class"))));
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

	protected static Vocab createVocab(SolrDocument solrDoc) {
		Vocab v = new Vocab();
		// get and set the fixed attributes
		v.setId(getString(solrDoc, "id"));
		v.setVocabMatch(getString(solrDoc, "phrase"));
		// TODO add "collection" to schema and loader
		v.setCollection("Generic");
		v.setCategory(internString(getString(solrDoc, "category")));
		v.setTaxonomy(internString(getString(solrDoc, "taxonomy")));

		// set any other atttributes from the solrdoc into the vocabMatches
		// attributes HashMap
		String[] pieces = vocabFieldNames.split(",");
		List<String> handled = new ArrayList<String>();
		for (String s : pieces) {
			handled.add(s);
		}

		Map<String, Object> tmpMap = solrDoc.getFieldValueMap();
		Map<String, Object> others = new HashMap<String, Object>();

		for (String s : tmpMap.keySet()) {
			if (!handled.contains(s)) {
				others.put(s, tmpMap.get(s));
			}

		}

		v.setAttributes(others);

		return v;
	}

}
