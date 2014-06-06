package org.opensextant.service;

import java.util.Properties;

import org.opensextant.service.processing.DocumentProcessorPool;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class OpenSextantApplication extends Application {

	// the pool of document processors
	DocumentProcessorPool dpPool;

	//properties
	Properties prop = null;

	public OpenSextantApplication(Properties prop) {
		this.prop = prop;
	}

	@Override
	public synchronized Restlet createInboundRoot() {

		// initialize the pool with settings in the property file
		dpPool = new DocumentProcessorPool(this.prop);
		System.out.println(dpPool.toString());

		// set up the routes
		Router router = new Router();

		// extraction route
		router.attach("/extract",OpenSextantExtractorResource.class);
		router.attach("/extract/",OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}",OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}/",OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}/{resultformat}",OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}/{resultformat}/",OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}/{resultformat}/url/{url}",OpenSextantExtractorResource.class);
		
		// lookup routes
		router.attach("/lookup/{format}/query/{query}",OpenSextantLookupResource.class);
		router.attach("/lookup/{format}/{placename}", OpenSextantLookupResource.class);
		router.attach("/lookup/{format}/{placename}/", OpenSextantLookupResource.class);
		router.attach("/lookup/{format}/{placename}/{country}",OpenSextantLookupResource.class);
		router.attach("/lookup/{format}/{placename}/{country}/",OpenSextantLookupResource.class);
		
		
		return router;
	}

	// accessor for the pool
	public DocumentProcessorPool getPool() {
		return this.dpPool;
	}

}
