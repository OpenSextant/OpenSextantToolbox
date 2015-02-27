package org.opensextant.service.processing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Queue;

import org.restlet.Client;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

public class ServiceClient implements Runnable {

  // parameters for the extractor
  private String extractHost;
  private int extractPort;
  private String extractType;
  private String extractFormat;

  // the client used to send request to the extractor
  protected Client client;

  // the queue which feeds files to this client
  private Queue<File> fileQueue;

  public ServiceClient(String extractHost, int extractPort, String extractType, String extractFormat) {
    this.extractHost = extractHost;
    this.extractPort = extractPort;
    this.extractType = extractType;
    this.extractFormat = extractFormat;
    this.client = new Client(Protocol.HTTP);
  }

  public ServiceClient(String extractHost) {
    this(extractHost, 8182, "general", "json");
  }

  @Override
  public void run() {

    // create reference to extractor
    Reference extractRef = new Reference(Protocol.HTTP, extractHost, extractPort);
    // add the extractor parameters
    extractRef = extractRef.addSegment("opensextant").addSegment("extract").addSegment(extractType)
        .addSegment(extractFormat);

    // create resource for extractor
    ClientResource extractResource = new ClientResource(extractRef);

    extractResource.setNext(client);

    while (!this.fileQueue.isEmpty()) {

      File srcFile = this.fileQueue.remove();
      // guess the mimetype of file (plain text by default)
      MediaType mt = MediaType.TEXT_PLAIN;
      try {
        String mtString = Files.probeContentType(srcFile.toPath());
        if (mtString != null) {
          mt = MediaType.valueOf(mtString);
        }
      } catch (IOException e1) {
        System.err.println("Couldn't get mimetype for " + srcFile.getName() + " using plain text");
      }

      // create representation for the input file
      Representation src = new FileRepresentation(srcFile, mt);

      try {
        // post file to extractor
        Representation extractResult = extractResource.post(src);

        Response resp = extractResource.getResponse();
        if (resp.getStatus().isSuccess()) {
          // do something with extraction results
          handleResults(extractResult);
        } else {
          System.err.println(resp);
        }

      } catch (ResourceException e) {
        e.printStackTrace();
      }

    }

    try {
      client.stop();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  // default result handler: dump to standard out, override in subclass
  protected void handleResults(Representation extractResult) {

    String txt;
    try {
      txt = extractResult.getText();
      if (txt == null || txt.length() < 100) {
        System.err.println("Bad response");
      } else {
        System.out.println(txt.length());
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public Queue<File> getFileQueue() {
    return fileQueue;
  }

  public void setFileQueue(Queue<File> fileQueue) {
    this.fileQueue = fileQueue;
  }

}
