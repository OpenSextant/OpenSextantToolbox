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
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSextantAdminResource extends ServerResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSextantAdminResource.class);
	static Set<String> operations = new HashSet<String>();
	static {
		operations.add("health");
		operations.add("shutdown");
	}

	/** Get a reference to the in the Application. */

	@Post
	@Put
	@Get
	public Representation doGet() {
		// get the request
		Request req = getRequest();
		// get the submitted attributes
		ConcurrentMap<String, Object> attrs = req.getAttributes();
		String op = (String) attrs.get("operation");

		if (op == null) {
			return new JacksonRepresentation<Set<String>>(operations);
		}

		if (!operations.contains(op)) {
			StringBuilder buf = new StringBuilder();
			buf.append("Unknown operation \"").append(op).append("\" requested. Supported operations are:");
			for (String s : operations) {
				buf.append(s).append(",");
			}
			buf.replace(buf.length() - 1, buf.length(), "");

			return new StringRepresentation(buf.toString());
		}

		if ("health".equalsIgnoreCase(op)) {
			DocumentProcessorPool dpPool = ((OpenSextantApplication) getApplication()).getPool();
			Map<String, Integer> avail = dpPool.available();

			long failCount = dpPool.getDocsFailedCount();
			long procCount = dpPool.getDocsProcessedCount();

			String stat = "green";

			if (avail.isEmpty()) {
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

			return new JsonRepresentation(ret);
		}

		if ("shutdown".equalsIgnoreCase(op)) {

			DeferedStop task = new DeferedStop();
			task.setApp((OpenSextantApplication) getApplication());
			task.setTime(4000);
			new Thread(task).start();

			return new StringRepresentation("Shutting down");

		}

		StringBuilder buf = new StringBuilder();
		buf.append("Unknown operation ").append(op).append(" requested. Supported operations are:");
		for (String s : operations) {
			buf.append(s).append(",");
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
