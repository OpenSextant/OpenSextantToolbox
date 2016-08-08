package org.opensextant.tagger.service;

import java.util.Properties;

import org.opensextant.tagger.TaggerPool;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSextantApplication extends Application {

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSextantApplication.class);

	/** The pool of document processors. */
	TaggerPool dpPool;

	/** Properties. */
	Properties prop;

	public OpenSextantApplication(Properties prop) {
		this.prop = prop;
	}

	@Override
	public synchronized Restlet createInboundRoot() {

		// initialize the pool with settings in the property file
		LOGGER.info("Initializing pool of extractors");
		dpPool = new TaggerPool(this.prop);
		LOGGER.info("Warming up extractor pool");
		// warm up the pool
		String content = "We drove to London.";
		for (String p : dpPool.getProcessNames()) {
			dpPool.tag(p, content);
		}

		LOGGER.info(dpPool.toString());

		// set up the routes
		Router router = new Router();

		// extraction route
		router.attach("/extract", OpenSextantExtractorResource.class);
		router.attach("/extract/", OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}", OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}/", OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}/{resultformat}", OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}/{resultformat}/", OpenSextantExtractorResource.class);
		router.attach("/extract/{extracttype}/{resultformat}/url/{url}", OpenSextantExtractorResource.class);

		// lookup routes
		router.attach("/lookup/{format}/query/{query}", OpenSextantLookupResource.class);
		router.attach("/lookup/{format}/{placename}", OpenSextantLookupResource.class);
		router.attach("/lookup/{format}/{placename}/", OpenSextantLookupResource.class);
		router.attach("/lookup/{format}/{placename}/{country}", OpenSextantLookupResource.class);
		router.attach("/lookup/{format}/{placename}/{country}/", OpenSextantLookupResource.class);

		// admin stuff
		router.attach("/admin", OpenSextantAdminResource.class);
		router.attach("/admin/", OpenSextantAdminResource.class);
		router.attach("/admin/{operation}", OpenSextantAdminResource.class);
		router.attach("/admin/{operation}/", OpenSextantAdminResource.class);

		return router;
	}

	/** Accessor for the pool. */
	public TaggerPool getPool() {
		return this.dpPool;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.Application#stop()
	 */
	@Override
	public synchronized void stop() throws Exception {
		this.dpPool.cleanup();
		super.stop();
	}

}
