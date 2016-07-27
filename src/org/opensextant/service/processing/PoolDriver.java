package org.opensextant.service.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolDriver {

  /** Log object. */
  private static final Logger LOGGER = LoggerFactory.getLogger(PoolDriver.class);

  private PoolDriver() {

  }

  public static void main(String[] args) {

    Properties prop = null;
    List<DocumentBean> holdem = new ArrayList<DocumentBean>();
    try {
      prop = new Properties();
      InputStream input = new FileInputStream(args[0]);

      // load properties file
      prop.load(input);
    } catch (FileNotFoundException e) {
      LOGGER.error("Couldn't load the properties file", e);
    } catch (IOException e) {
      LOGGER.error("Couldn't load the properties file", e);
    }

    DocumentProcessorPool dpPool = new DocumentProcessorPool(prop);

    File inDir = new File(args[1]);

    Collection<File> filesToProcess = FileUtils.listFiles(inDir, FileFilterUtils.trueFileFilter(),
        FileFilterUtils.trueFileFilter());

    for (File f : filesToProcess) {
      DocumentBean result = dpPool.process("general", f);
      LOGGER.info(f.getName());
      holdem.add(result);
    }

    dpPool.cleanup();
    dpPool = null;

  }

  public static void dump(DocumentBean doc) {
    LOGGER.info(doc.getContent());
  }
}
