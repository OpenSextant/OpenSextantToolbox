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
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.solr.client.solrj.SolrServer;
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

  // Log object
  private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);

  // some common params
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

    if (args.length < 3 || args.length > 4) {
      usage();
    }

    String scheme = args[0];
    String inputForm = args[1];
    String csvFilePath = args[2];
    String solrhome = "";
    if (args.length == 4) {
      solrhome = args[3];
    }

    // get a SolrServer with the proper core
    SolrServer solrServer = getSolrServer(scheme, solrhome);

    // convert indexed content to flat list
    // currently creates a temp file, could stream?
    if (inputForm.equalsIgnoreCase("index")) {
      csvFilePath = flatten(csvFilePath);
    }

    try {

      // set the fieldnames param for the selected schema
      final ModifiableSolrParams params = new ModifiableSolrParams(loadParams);
      if (scheme.equalsIgnoreCase("gazetteer")) {
        params.set("fieldnames", MatcherFactory.getGazetteerFieldNamesLoader());
      } else {
        params.set("fieldnames", MatcherFactory.getVocabFieldNames());
      }

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
      solrServer.shutdown();
    }
  }

  private static void usage() {
    String tmp = "DataLoader <scheme> <inputformat> <inputfilepath> <solrhome> where\n";
    tmp = tmp + " <scheme> = gazetteer | vocabulary\n";
    tmp = tmp + " <inputformat> = csv | index\n";
    tmp = tmp + " <inputfilepath> = file to be loaded\n";
    tmp = tmp + " <solrhome> = path to solr home (optional)\n";

    LOGGER.info(tmp);
  }

  private static String flatten(String currentPath) {

    File topDir = new File(currentPath).getParentFile();

    File input = new File(currentPath);

    Map<File, String> index = new HashMap<File, String>();

    // read the index file into the index Map

    // loop over the lines of the index file
    LineIterator indexIter = null;
    try {
      indexIter = FileUtils.lineIterator(input, "UTF-8");
    } catch (IOException e) {
      LOGGER.error("Couldnt read from " + input.getName() + ":", e);
      return null;
    }

    if (indexIter != null) {
      while (indexIter.hasNext()) {
        // get next line
        String line = indexIter.next();
        String[] pieces = line.split(":");
        File subFile = new File(topDir, pieces[0]);
        String tmpVal = pieces[1];

        if (pieces.length >= 3) {
          tmpVal = tmpVal + ":" + pieces[2];
        }

        index.put(subFile, tmpVal);

      }
    }
    File tmp = null;
    try {
      tmp = File.createTempFile("vocab", "txt");
    } catch (IOException e) {
      LOGGER.error("Could not create temp file when flattening vocab:", e);
      return null;
    }

    // loop over the files mentioned in the index and write to temp file
    int indexID = 0;
    for (File in : index.keySet()) {
      String[] catAndTax = index.get(in).split(":");
      String cat = catAndTax[0];
      String tax = "";
      if (catAndTax.length > 1) {
        tax = catAndTax[1];
      } else {
        tax = "NONE";
      }

      // loop over the lines of the subfiles file
      // write the new flat contents to the temp file
      LineIterator contentIter = null;
      try {
        contentIter = FileUtils.lineIterator(in, "UTF-8");
      } catch (IOException e) {
        LOGGER.error("Couldnt read from " + in.getName(), e);
        return null;
      }

      if (contentIter != null) {
        while (contentIter.hasNext()) {
          // get next line
          String line = contentIter.next();

          // concat the pieces
          String out = indexID + "\t" + line + "\t" + cat + "\t" + tax + "\n";

          // write all pieces to temp

          try {
            FileUtils.writeStringToFile(tmp, out, "UTF-8", true);
          } catch (IOException e) {
            LOGGER.error("Could not write to temp file when flattening vocab:", e);
          }
          indexID++;

        }
      }
    }
    LOGGER.info("Flattened " + indexID + " vocabulary entries to temp file");
    // return temp file path

    return tmp.getAbsolutePath();
  }

  private static void printResponse(SolrResponseBase response) {
    LOGGER.info(response.toString());
  }

  private static SolrServer getSolrServer(String scheme, String solrhome) {

    MatcherFactory.config(solrhome);
    MatcherFactory.start();

    SolrServer svr = null;

    if (scheme.equalsIgnoreCase("gazetteer")) {
      svr = MatcherFactory.getSolrServerGeo();
    } else {
      svr = MatcherFactory.getSolrServerVocab();
    }

    return svr;

  }

}
