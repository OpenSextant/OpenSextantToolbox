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

public class OpenSextantLookupResource extends ServerResource {

  @Get()
  public Representation doGet() {

    Request req = this.getRequest();
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

    System.out.println("Query is" + query);
    List<Place> placesFound = s.searchByQueryString(query);
    System.out.println("Found " + placesFound.size() + " places");

    if (format.equalsIgnoreCase("json")) {
      JacksonRepresentation<List<Place>> jackRep = new JacksonRepresentation<List<Place>>(MediaType.APPLICATION_JSON,
          placesFound);
      return jackRep;
    }

    if (format.equalsIgnoreCase("csv")) {
      StringBuffer buff = new StringBuffer();

      buff.append("PlaceName\tExpandedPlaceName\tNameType\tNameTypeSystem\tCountryCode\tAdmin1\tAdmin2\tFeatureClass\tFeatureCode\tLatitude\tLongitude\tSource\n");

      for (Place pl : placesFound) {
        buff.append(ifNull(pl.getPlaceName()) + "\t");
        buff.append(ifNull(pl.getExpandedPlaceName()) + "\t");
        buff.append(ifNull(pl.getNameType()) + "\t");
        buff.append(ifNull(pl.getNameTypeSystem()) + "\t");
        buff.append(ifNull(pl.getCountryCode()) + "\t");
        buff.append(ifNull(pl.getAdmin1()) + "\t");
        buff.append(ifNull(pl.getAdmin2()) + "\t");
        buff.append(ifNull(pl.getFeatureClass()) + "\t");
        buff.append(ifNull(pl.getFeatureCode()) + "\t");
        buff.append(ifNull(pl.getLatitude().toString()) + "\t");
        buff.append(ifNull(pl.getLongitude().toString()) + "\t");
        buff.append(ifNull(pl.getSource()) + "\t");
        buff.append("\n");
      }

      StringRepresentation rep = new StringRepresentation(buff.toString());

      return rep;
    }

    return new StringRepresentation("Unknown format:" + format);

  }

  @Put
  public Representation doPut(Representation entity) throws Exception {

    return handle(this.getRequest());
  }

  @Post()
  public Representation doPost(Representation entity) throws Exception {

    return handle(this.getRequest());
  }

  private Representation handle(Request req) {

    ConcurrentMap<String, Object> attrs = req.getAttributes();
    Representation ent = req.getEntity();

    RestletFileUpload fileupload = new RestletFileUpload(new DiskFileItemFactory());
    List<FileItem> fileItems = null;
    try {
      fileItems = fileupload.parseRepresentation(ent);
    } catch (FileUploadException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    for (FileItem fileItem : fileItems) {
      String fieldName = fileItem.getFieldName();
      String contentType = fileItem.getContentType();
      System.out.println("fieldname=" + fieldName);
      System.out.println("contentType =" + contentType);
      InputStream is = null;
      try {
        is = fileItem.getInputStream();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (is != null) {
        Representation in = new InputRepresentation(is);
        try {
          in.write(System.out);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    String meth = req.getMethod().getName();
    String attrString = "";
    for (String n : attrs.keySet()) {
      attrString = attrString + "\n" + n + "=" + attrs.get(n).toString();
    }
    String extractType = (String) attrs.get("type");
    String format = (String) attrs.get("format");
    String content = (String) attrs.get("content");

    String retString = "You requested a " + meth + " Lookup with attributes= " + attrString;

    Representation ret = new StringRepresentation(retString);

    return ret;
  }

  private String ifNull(String in) {
    if (in == null) {
      return "";
    }

    return in;
  }

}
