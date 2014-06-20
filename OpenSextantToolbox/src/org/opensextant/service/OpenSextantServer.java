package org.opensextant.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.restlet.Component;
import org.restlet.data.Protocol;

public class OpenSextantServer {

  public static void main(String[] args) throws Exception {

    Properties prop = new Properties();
    InputStream input = new FileInputStream(args[0]);

    // load properties file
    prop.load(input);

    // Create a new Component.
    Component component = new Component();

    // Add a new HTTP server listening on port 8182.
    component.getServers().add(Protocol.HTTP, 8182);

    // Attach the application.
    OpenSextantApplication app = new OpenSextantApplication(prop);

    // set the topmost route
    component.getDefaultHost().attach("/opensextant", app);

    // Start the component.
    component.start();
    System.out.println("OpenSextant REST server is ready");
  }

}
