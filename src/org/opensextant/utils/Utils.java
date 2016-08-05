package org.opensextant.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

	public static List<String> getCoreNames(SolrClient client) {

		List<String> coreList = new ArrayList<String>();

		CoreAdminRequest request = new CoreAdminRequest();
		request.setAction(CoreAdminAction.STATUS);
		try {
			CoreAdminResponse cores = request.process(client);

			// List of the cores

			for (int i = 0; i < cores.getCoreStatus().size(); i++) {
				coreList.add(cores.getCoreStatus().getName(i));
			}

			return coreList;

		} catch (SolrServerException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return coreList;

	}

	public static String getFieldNames(SolrClient client, String coreName) {

		StringBuilder fieldNames = new StringBuilder();
		SchemaRequest req = new SchemaRequest();
		SchemaRepresentation rep = null;
		SchemaResponse resp = null;

		try {
			resp = req.process(client);
		} catch (SolrServerException | IOException e) {
			LOGGER.error("Could not get field names:", e);
		}

		rep = resp.getSchemaRepresentation();

		List<Map<String, Object>> fields = rep.getFields();

		for (Map<String, Object> field : fields) {

			String name = (String) field.get("name");

			fieldNames.append(name);
			fieldNames.append(",");
		}

		return fieldNames.toString();

	}

	/**
	 * @param in
	 *            a string to be interned
	 * @return the interned string
	 */
	public static String internString(String in) {
		if (in != null) {
			return in.intern();
		}
		return in;
	}

	public static String convertFile(File file) {
		return "";
	};

	public static String convertURL(URL url) {
		return "";
	};

	/**
	 * Get a String object from a record.
	 */
	public static String getString(SolrDocument solrDoc, String name) {
		Object result = solrDoc.getFirstValue(name);
		if (result != null) {
			return result.toString();
		}
		return null;
	}

	/**
	 * Get a double from a record.
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
	 * 
	 * @return XY double array, [lat, lon]
	 */
	public static double[] getCoordinate(SolrDocument solrDoc, String field) {
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

}
