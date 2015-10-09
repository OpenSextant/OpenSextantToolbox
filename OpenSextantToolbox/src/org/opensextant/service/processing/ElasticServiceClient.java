package org.opensextant.service.processing;

import java.io.IOException;

import org.opensextant.service.OpenSextantApplication;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticServiceClient extends ServiceClient {

  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSextantApplication.class);
  private String storeHost;
  private int storePort = 9200;
  private String storeIndex = "extractions";
  private String storeType = "extract";

  public ElasticServiceClient(String extractHost, String extractType, String esHost) {
    super(extractHost, 9200, extractType, "json");
    this.storeHost = esHost;
  }

  public ElasticServiceClient(String extractHost, String esHost) {
    super(extractHost);
    this.storeHost = esHost;
  }

  @Override
  protected void handleResults(Representation extractResult) {

    Reference storeRef = new Reference(Protocol.HTTP, storeHost, storePort);
    storeRef = storeRef.addSegment(storeIndex).addSegment(storeType);

    ClientResource storeResource = new ClientResource(storeRef);
    storeResource.setNext(client);

    try {
      InputRepresentation extractRep = new InputRepresentation(extractResult.getStream(), MediaType.TEXT_PLAIN);
    } catch (IOException e) {
      LOGGER.warn("Could not handle result", e);
    }

  }

}
