package org.opensextant.service.processing;

import java.io.IOException;

import org.restlet.Client;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

public class ElasticServiceClient extends ServiceClient {

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

    InputRepresentation extractRep;
    try {
      extractRep = new InputRepresentation(extractResult.getStream(), MediaType.TEXT_PLAIN);
      Representation storeResult = storeResource.post(extractRep, MediaType.APPLICATION_JSON);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
