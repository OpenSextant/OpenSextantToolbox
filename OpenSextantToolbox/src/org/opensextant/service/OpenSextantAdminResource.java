package org.opensextant.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONException;
import org.json.JSONStringer;
import org.opensextant.service.processing.DocumentProcessorPool;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSextantAdminResource extends ServerResource {

  static Set<String> operations = new HashSet<String>();
  static {
    operations.add("health");
    operations.add("shutdown");
  }

  @Override
  protected void doInit() throws ResourceException {
    super.doInit();
  }
    // get a reference to the in the Application

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSextantAdminResource.class);

  @Post
  @Put
  @Get
  public Representation doGet() {
    // get the request
    Request req = this.getRequest();
    // get the submitted attributes
    ConcurrentMap<String, Object> attrs = req.getAttributes();
    String op = (String) attrs.get("operation");

    if (op == null) {
      JacksonRepresentation<Set<String>> jackRep = new JacksonRepresentation<Set<String>>(operations);
      return jackRep;
    }

    if (!operations.contains(op)) {
      StringBuffer buf = new StringBuffer();
      buf.append("Unknown operation \"" + op + "\" requested. Supported operations are:");
      for (String s : operations) {
        buf.append(s + ",");
      }
      buf.replace(buf.length() - 1, buf.length(), "");

      return new StringRepresentation(buf.toString());
    }

    if (op.equalsIgnoreCase("health")) {
      DocumentProcessorPool dpPool = ((OpenSextantApplication) this.getApplication()).getPool();
      Map<String, Integer> avail = dpPool.available();

      long failCount  = dpPool.getDocsFailedCount();
      long procCount = dpPool.getDocsProcessedCount();

      String stat = "green";

      if(avail.size() == 0){
        stat = "red";
      }

      JSONStringer ret = new JSONStringer();
      try {
        ret.object();
        ret.key("status").value(stat);
        ret.key("documentsProcessed").value(procCount);
        ret.key("documentsFailed").value(failCount);
        ret.key("availableProcessors").value(avail);
        ret.endObject();
      } catch (JSONException e) {
        LOGGER.error("JSON exception when attemting to create status info ", e);
      }

      JsonRepresentation jsonRep = new JsonRepresentation(ret);

      return jsonRep;
    }

    if (op.equalsIgnoreCase("shutdown")) {

      DeferedStop task = new DeferedStop();
      task.setApp((OpenSextantApplication) this.getApplication());
      task.setTime(4000);
      new Thread(task).start();

      return new StringRepresentation("Shutting down");

    }

    StringBuffer buf = new StringBuffer();
    buf.append("Unknown operation " + op + " requested. Supported operations are:");
    for (String s : operations) {
      buf.append(s + ",");
    }

    return new StringRepresentation(buf.toString());

  }

  class DeferedStop implements Runnable {
    private OpenSextantApplication app;
    private int t = 4000;

    public void setApp(OpenSextantApplication osApp) {
      this.app = osApp;
    }

    public void setTime(int t) {
      this.t = t;
    }

    @Override
    public void run() {

      try {
        Thread.sleep(t);
      } catch (InterruptedException ex) {
        // eat exception
      } finally {

        try {
          this.app.stop();
          Component comp = (Component) app.getContext().getAttributes().get("component");
          comp.stop();
        } catch (Exception e) {
          LOGGER.error("Couldnt handle provided form", e);
        }

      }
    }
  }
}
