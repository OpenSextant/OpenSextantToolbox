/*
 This software was produced for the U. S. Government
 under Contract No. W15P7T-11-C-F600, and is
 subject to the Rights in Noncommercial Computer Software
 and Noncommercial Computer Software Documentation
 Clause 252.227-7014 (JUN 1995)

 Copyright 2013 The MITRE Corporation. All Rights Reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.opensextant.matching;

import java.io.File;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataLoader {

	private static ModifiableSolrParams loadParams = new ModifiableSolrParams();
	private static String requestHandler = "/update";

	/** Log object. */
	private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);

	/** Some common params. */
	static {
		loadParams.set("update.contentType", "text/csv");
		loadParams.set("skipLines", "1");
		loadParams.set("optimize", "true");
		loadParams.set("separator", "\t");
		loadParams.set("header", "false");
		loadParams.set("trim", "on");
		loadParams.set("overwrite", "false");
		loadParams.set("debug", "true");
	}

	private DataLoader() {
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 1 || args.length > 2) {
			usage();
		}

		String csvFilePath = args[0];
		String solrhome = "";
		if (args.length == 2) {
			solrhome = args[1];
		}

		// get a SolrServer
		SolrClient solrServer = getSolrServer(solrhome);

		try {

			// set the fieldnames param for the selected schema
			final ModifiableSolrParams params = new ModifiableSolrParams(loadParams);

			params.set("fieldnames", MatcherFactory.getGazetteerFieldNamesLoader());

			// build the update request
			final ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(requestHandler);
			updateRequest.setParams(params);

			ContentStream inStream = new ContentStreamBase.FileStream(new File(csvFilePath));

			// add the input file as a stream to the request
			updateRequest.addContentStream(inStream);

			// make the call
			SolrResponseBase response = null;
			try {
				response = updateRequest.process(solrServer);
				// see what happened
				printResponse(response);
			} catch (Exception e) {
				LOGGER.error("Exception in submitting Solr request " + e);
			}

		} finally {
			// cleanup
			solrServer.close();
		}
	}

	private static void usage() {
		String tmp = "DataLoader  <inputfilepath> <solrhome> where\n";
		tmp = tmp + " <inputfilepath> = CSV file to be loaded\n";
		tmp = tmp + " <solrhome> = path to solr home (optional)\n";

		LOGGER.info(tmp);
	}

	private static void printResponse(SolrResponseBase response) {
		LOGGER.info(response.toString());
	}

	private static SolrClient getSolrServer(String solrhome) {

		MatcherFactory.config(solrhome);
		MatcherFactory.start();

		SolrClient svr = null;

		svr = MatcherFactory.getSolrServerGeo();

		return svr;

	}

}
