package org.opensextant.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.opensextant.placedata.Geocoord;
import org.opensextant.placedata.Place;
import org.opensextant.tagger.Document;
import org.opensextant.tagger.Match;
import org.opensextant.tagger.TaggerPool;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSextantExtractorResource extends ServerResource {

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSextantApplication.class);

	/** The name of the field in the form which holds the uploaded file. */
	static String formFileName = "infile";
	/** The result formats supported. */
	static Set<String> formats = new HashSet<String>();
	static {
		formats.add("json");
		formats.add("geojson");
		formats.add("xml");
		formats.add("csv");
	}

	/** The pool from which the document processor is pulled. */
	TaggerPool dpPool;

	@Override
	protected void doInit() {
		super.doInit();
		// get a reference to the pool in the Application
		dpPool = ((OpenSextantApplication) getApplication()).getPool();
	}

	@Get
	public Representation doGet() {
		return new StringRepresentation("GET is not supported, use POST or PUT");
	}

	@Post
	@Put
	public Representation doPost(Representation entity) throws IOException {
		// get the request
		Request req = getRequest();
		// get the submitted attributes
		ConcurrentMap<String, Object> attrs = req.getAttributes();
		String type = (String) attrs.get("extracttype");
		String format = (String) attrs.get("resultformat");
		String sourceURLString = (String) attrs.get("url");

		if (sourceURLString != null) {
			String sourceURL = java.net.URLDecoder.decode(sourceURLString, "UTF-8");
			URL url = new URL(sourceURL);
			return extract(type, format, url);
		}

		// return list of extraction types
		if (type == null) {
			Set<String> ret = this.dpPool.getProcessNames();
			return new JacksonRepresentation<Set<String>>(ret);
		}

		// return list of result types
		if (format == null) {
			return new JacksonRepresentation<Set<String>>(formats);
		}

		if (entity != null) {
			MediaType media = entity.getMediaType();

			if (media == null) {// bare stream?
				InputStream ios = entity.getStream();

				URL url = streamUrl("http://infile", ios, null);
				return extract(type, format, url);
			}
			// if its a form
			if (MediaType.MULTIPART_FORM_DATA.equals(media, true)) {
				URL u = handleForm(entity, formFileName);
				if (u != null) {
					return extract(type, format, u);
				}
				return new StringRepresentation("Form with no field named  \"" + formFileName + "\"");
			}

			// if it is text of some kind
			if (MediaType.TEXT_PLAIN.equals(media, true) || MediaType.TEXT_XML.equals(media, true)
					|| MediaType.TEXT_HTML.equals(media, true) || MediaType.APPLICATION_XML.equals(media, true)) {
				return extract(type, format, entity.getText());
			}

			// if it WWW form
			if (MediaType.APPLICATION_WWW_FORM.equals(media, true)) {
				return extract(type, format, entity.getText());
			}

			return new StringRepresentation("POST or PUT requested but can't handle " + media.getName() + " type body");

		} else {
			return new StringRepresentation("POST or PUT requested but no body provided");
		}

	}

	/** Extract the content from the submitted form as a URL. */
	private URL handleForm(Representation entity, String filename) {

		RestletFileUpload fileupload = new RestletFileUpload(new DiskFileItemFactory());
		List<FileItem> fileItems = null;
		try {
			fileItems = fileupload.parseRepresentation(entity);
		} catch (FileUploadException e) {
			LOGGER.error("Couldnt handle provided form", e);
		}

		// look for the field containing the file
		for (FileItem fileItem : fileItems) {

			if (fileItem.getFieldName().equalsIgnoreCase(filename)) {
				String fn = fileItem.getName();
				try {

					File tmpFile = File.createTempFile("ossvr", fn);
					tmpFile.deleteOnExit();
					fileItem.write(tmpFile);
					return tmpFile.toURI().toURL();

				} catch (MalformedURLException e) {
					LOGGER.error("Couldnt handle provided form", e);
					return null;
				} catch (IOException e) {
					LOGGER.error("Couldnt handle provided form", e);
					return null;
				} catch (Exception e) {
					LOGGER.error("Couldnt handle provided form", e);
					return null;
				}

			}

		} // end fileitems loop
			// didnt find a field of the correct name
		return null;
	}

	private Representation extract(String extractType, String resultFormat, String content) {

		if (dpPool.getProcessNames().contains(extractType)) {
			Document result = dpPool.tag(extractType, content);

			return convertResult(result, resultFormat);
		} else {
			return new StringRepresentation("Unknown extraction type:" + extractType);
		}
	}

	private Representation extract(String extractType, String resultFormat, URL content) {

		if (dpPool.getProcessNames().contains(extractType)) {
			Document doc = dpPool.tag(extractType, content);

			// clean up temp file if used
			if ("file".equalsIgnoreCase(content.getProtocol())) {
				String tempFilePath = content.getPath();
				File tmpFile = new File(tempFilePath);
				if (!tmpFile.delete()) {
					LOGGER.error("Unable to delete temp file" + tmpFile.getPath());
				}
			}

			if (doc != null) {
				return convertResult(doc, resultFormat);
			} else {
				return new StringRepresentation("Couldnt extract content from:" + content.toExternalForm());
			}

		} else {
			return new StringRepresentation("Unknown extraction type:" + extractType);
		}
	}

	private Representation convertResult(Document db, String resultFormat) {

		if ("json".equalsIgnoreCase(resultFormat)) {
			return new JacksonRepresentation<Document>(db);
		}

		if ("geojson".equalsIgnoreCase(resultFormat)) {
			FeatureCollection coll = new FeatureCollection();

			for (Match a : db.getMatchList()) {
				Feature ft = new Feature();
				String t = a.getType();
				Map<String, Object> fm = a.getFeatures();
				Object h = fm.get("hierarchy");

				ft.setProperty("matchtext", a.getMatchText());
				ft.setProperty("entitytype", t);
				ft.setProperty("hierarchy", h);
				ft.setProperty("start", a.getStart());
				ft.setProperty("end", a.getEnd());
				ft.setProperty("snippet", db.getSnippet(a, 25));
				ft.setGeometry(null);

				if ("Date".equalsIgnoreCase(t)) {
					Date dt = (Date) fm.get("date");
					ft.setProperty("date", dt.toString());
				}

				if ("PLACE".equalsIgnoreCase(t)) {
					Place pl = (Place) fm.get("place");
					ft.setProperty("placeName", pl.getPlaceName());
					ft.setProperty("countrycode", pl.getCountryCode());
					ft.setProperty("featureclass", pl.getFeatureClass());
					ft.setProperty("featurecode", pl.getFeatureCode());
					Point pt = new Point(pl.getLongitude(), pl.getLatitude());
					ft.setGeometry(pt);
				}

				if ("GEOCOORD".equalsIgnoreCase(t)) {
					Geocoord geo = (Geocoord) fm.get("geo");
					Point pt = new Point(geo.getLongitude(), geo.getLatitude());
					ft.setGeometry(pt);
				}

				coll.add(ft);
			}

			return new JacksonRepresentation<FeatureCollection>(coll);
		}

		if ("xml".equalsIgnoreCase(resultFormat)) {
			return new JacksonRepresentation<Document>(MediaType.TEXT_XML, db);
		}

		if ("csv".equalsIgnoreCase(resultFormat)) {

			StringBuilder buff = new StringBuilder();

			buff.append(
					"MatchText\tType\tHierarchy\tStart\tEnd\tSnippet\tDate\tPlaceName\tCountryCode\tFeatureClass\tFeatureCode\tLatitude\tLongitude\n");

			for (Match a : db.getMatchList()) {
				String t = a.getType();
				Map<String, Object> fm = a.getFeatures();
				Object h = fm.get("hierarchy");

				buff.append(a.getMatchText()).append("\t").append(t).append("\t").append(h).append("\t")
						.append(a.getStart()).append("\t").append(a.getEnd()).append("\t");
				buff.append(db.getSnippet(a, 25));

				if ("Date".equalsIgnoreCase(t)) {
					Date dt = (Date) fm.get("date");
					buff.append("\t");
					buff.append(dt).append("\t");
				} else {
					buff.append("\t");
				}

				if ("PLACE".equalsIgnoreCase(t)) {
					Place pl = (Place) fm.get("place");
					buff.append("\t");
					buff.append(pl.getPlaceName()).append("\t");
					buff.append(pl.getCountryCode()).append("\t");
					buff.append(pl.getFeatureClass()).append("\t");
					buff.append(pl.getFeatureCode()).append("\t");
					buff.append(pl.getLatitude()).append("\t");
					buff.append(pl.getLongitude()).append("\t");

				}
				if ("GEOCOORD".equalsIgnoreCase(t)) {
					Geocoord geo = (Geocoord) fm.get("geo");
					buff.append("\t");
					buff.append("\t");
					buff.append("\t");
					buff.append("\t");
					buff.append("\t");
					buff.append(geo.getLatitude()).append("\t");
					buff.append(geo.getLongitude()).append("\t");
				}
				buff.append("\n");
			}

			return new StringRepresentation(buff.toString());
		}

		return new JacksonRepresentation<Document>(db);
	}

	/** Create a URL based on an InputStream. */
	private URL streamUrl(String urlString, final InputStream is, final String contType) throws MalformedURLException {
		return new URL(null, urlString, new URLStreamHandler() {
			@Override
			public URLConnection openConnection(URL u) {
				return new URLConnection(u) {
					@Override
					public String getContentType() {
						return contType;
					}

					@Override
					public void connect() {
						// do nothing
					}

					@Override
					public InputStream getInputStream() {
						return is;
					}
				};
			}
		});
	}

	public static Set<String> getFormats() {
		return formats;
	}

}
