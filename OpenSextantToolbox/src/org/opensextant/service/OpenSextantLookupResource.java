package org.opensextant.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.opensextant.matching.MatcherFactory;
import org.opensextant.matching.PlacenameSearcher;
import org.opensextant.placedata.Place;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.InputRepresentation;
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

  @Get
  public Representation doGet() {

    Request req = getRequest();
    // get the submitted attributes
    ConcurrentMap<String, Object> attrs = req.getAttributes();
    String format = (String) attrs.get("format");
    String placeName = (String) attrs.get("placename");
    String country = (String) attrs.get("country");
    String rawQuery = (String) attrs.get("query");

    PlacenameSearcher s = MatcherFactory.getSearcher();
    String query;

    if (rawQuery == null) {
      query = "name:" + placeName;
      if (country != null) {
        query = query + " AND cc:" + country;
      }
    } else {
      query = Reference.decode(rawQuery);
    }

    LOGGER.info("Query is" + query);
    List<Place> placesFound = s.searchByQueryString(query);
    LOGGER.info("Found " + placesFound.size() + " places");

    if ("json".equalsIgnoreCase(format)) {
      return new JacksonRepresentation<List<Place>>(MediaType.APPLICATION_JSON, placesFound);
    }

    if ("csv".equalsIgnoreCase(format)) {
      StringBuilder buff = new StringBuilder();

      buff.append("PlaceName\tExpandedPlaceName\tNameType\tNameTypeSystem\tCountryCode\tAdmin1\tAdmin2\tFeatureClass\tFeatureCode\tLatitude\tLongitude\tSource\n");

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

    return handle(getRequest());
  }

  @Post
  public Representation doPost() {

    return handle(getRequest());
  }

  private Representation handle(Request req) {

    ConcurrentMap<String, Object> attrs = req.getAttributes();
    Representation ent = req.getEntity();

    RestletFileUpload fileupload = new RestletFileUpload(new DiskFileItemFactory());
    List<FileItem> fileItems = null;
    try {
      fileItems = fileupload.parseRepresentation(ent);
    } catch (FileUploadException e) {
      LOGGER.error("Couldn't parse request reprsentation", e);
    }

    for (FileItem fileItem : fileItems) {
      String fieldName = fileItem.getFieldName();
      String contentType = fileItem.getContentType();
      LOGGER.info("fieldname=" + fieldName);
      LOGGER.info("contentType =" + contentType);
      InputStream is = null;
      try {
        is = fileItem.getInputStream();
      } catch (IOException e) {
        LOGGER.error("Couldn't handle request, couldn't open stream", e);
      }
      if (is != null) {
        Representation in = new InputRepresentation(is);
        try {
          in.write(System.out);
        } catch (IOException e) {
          LOGGER.error("Couldn't write Representation", e);
        }
      }
    }

    String meth = req.getMethod().getName();
    String attrString = "";
    for (String n : attrs.keySet()) {
      attrString = attrString + "\n" + n + "=" + attrs.get(n);
    }

    String retString = "You requested a " + meth + " Lookup with attributes= " + attrString;

    return new StringRepresentation(retString);
  }

  private String ifNull(String in) {
    if (in != null) {
      return in;
    }

    return "";
  }

}
