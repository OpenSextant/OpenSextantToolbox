package org.opensextant.tagger.service;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.opensextant.tagger.TaggerPool;
import org.opensextant.tagger.geo.Place;
import org.opensextant.tagger.solr.GeoSolrTagger;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSextantLookupResource extends ServerResource {

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSextantLookupResource.class);

	/** The pool from which the document processor is pulled. */
	TaggerPool dpPool;
	String taggerType = "geogazetteer";

	@Override
	protected void doInit() {
		super.doInit();
		// get a reference to the pool in the Application
		dpPool = ((OpenSextantApplication) getApplication()).getPool();
	}

	@Get
	public Representation doGet() {

		Request req = getRequest();
		// get the submitted attributes
		ConcurrentMap<String, Object> attrs = req.getAttributes();
		String format = (String) attrs.get("format");
		String placeName = (String) attrs.get("placename");
		String country = (String) attrs.get("country");
		String rawQuery = (String) attrs.get("query");

		LOGGER.debug("Got a request:" + attrs);

		GeoSolrTagger tagger = null;
		if (dpPool.getProcessNames().contains(taggerType)) {
			tagger = (GeoSolrTagger) dpPool.getTagger(taggerType);
			if (tagger == null) {
				LOGGER.error("Could not get a " + taggerType + " Gazetteer");
				return new StringRepresentation("Could not get a " + taggerType + " Gazetteer");
			}

		} else {
			dpPool.returnTagger(tagger);
			LOGGER.error("Could not get a " + taggerType + " Gazetteer");
			return new StringRepresentation("Could not get a " + taggerType + " Gazetteer");
		}

		List<Place> placesFound = tagger.geoQueryByName(placeName, false);

		dpPool.returnTagger(tagger);

		LOGGER.info("Found " + placesFound.size() + " places");

		if ("json".equalsIgnoreCase(format)) {
			return new JacksonRepresentation<List<Place>>(MediaType.APPLICATION_JSON, placesFound);
		}

		if ("csv".equalsIgnoreCase(format)) {
			StringBuilder buff = new StringBuilder();

			buff.append(
					"PlaceName\tExpandedPlaceName\tNameType\tNameTypeSystem\tCountryCode\tAdmin1\tAdmin2\tFeatureClass\tFeatureCode\tLatitude\tLongitude\tSource\n");

			for (Place pl : placesFound) {
				buff.append(ifNull(pl.getPlaceName())).append("\t");
				buff.append(ifNull(pl.getExpandedPlaceName())).append("\t");
				buff.append(ifNull(pl.getNameType())).append("\t");
				buff.append(ifNull(pl.getNameTypeSystem())).append("\t");
				buff.append(ifNull(pl.getCountryCode())).append("\t");
				buff.append(ifNull(pl.getAdmin1())).append("\t");
				buff.append(ifNull(pl.getAdmin2())).append("\t");
				buff.append(ifNull(pl.getFeatureClass())).append("\t");
				buff.append(ifNull(pl.getFeatureCode())).append("\t");
				buff.append(ifNull(pl.getLatitude().toString())).append("\t");
				buff.append(ifNull(pl.getLongitude().toString())).append("\t");
				buff.append(ifNull(pl.getSource())).append("\t");
				buff.append("\n");
			}

			return new StringRepresentation(buff.toString());
		}

		return new StringRepresentation("Unknown format:" + format);

	}

	@Put
	public Representation doPut() {

		return new StringRepresentation("PUT is not supported, use GET");
	}

	@Post
	public Representation doPost() {

		return new StringRepresentation("POST is not supported, use GET");
	}

	private String ifNull(String in) {
		if (in != null) {
			return in;
		}

		return "";
	}

}
