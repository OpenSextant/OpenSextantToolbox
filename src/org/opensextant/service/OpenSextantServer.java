package org.opensextant.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSextantServer {

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSextantServer.class);

	private OpenSextantServer() {

	}

	public static void main(String[] args) throws Exception {

		Properties prop = new Properties();
		InputStream input = new FileInputStream(args[0]);

		// load properties file
		prop.load(input);

		// Create a new Component.
		Component component = new Component();

		// get port from environment variable
		String port_env = System.getenv("PORT");
		int port = 8182;
		if(port_env != null) {
			port = Integer.parseInt(port_env);
		}

		// Add a new HTTP server listening on port.
		Server srvr = new Server(Protocol.HTTP, port);
		component.getServers().add(srvr);

		// get some server properties or defaults
		String minThreads = prop.getProperty("os.service.server.minThreads", "1");
		String maxThreads = prop.getProperty("os.service.server.maxThreads", "10");
		String maxQueued = prop.getProperty("os.service.server.maxQueued", "-1");
		String maxIdle = prop.getProperty("os.service.server.maxIdle", "300000");

		// set the server parameters
		srvr.getContext().getParameters().add("minThreads", minThreads);
		srvr.getContext().getParameters().add("maxThreads", maxThreads);
		srvr.getContext().getParameters().add("maxQueued", maxQueued);
		srvr.getContext().getParameters().add("maxThreadIdleTimeMs", maxIdle);

		// Attach the application.
		OpenSextantApplication app = new OpenSextantApplication(prop);

		// set the topmost route
		component.getDefaultHost().attach("/opensextant", app);
		app.getContext().getAttributes().put("component", component);

		// Start the component.
		component.start();
		LOGGER.info("OpenSextant REST server is ready");
	}

}
