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

public class PoolDriver {

  private PoolDriver() {

  }

  /**
   * @param args
   */
  public static void main(String[] args) {

    Properties prop = null;
    List<DocumentBean> holdem = new ArrayList<DocumentBean>();
    try {
      prop = new Properties();
      InputStream input = new FileInputStream(args[0]);

      // load properties file
      prop.load(input);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    DocumentProcessorPool dpPool = new DocumentProcessorPool(prop);

    File inDir =  new File(args[1]);

    Collection<File> filesToProcess = FileUtils.listFiles(inDir,
        FileFilterUtils.trueFileFilter(),
        FileFilterUtils.trueFileFilter());

   // String content = " They were attacked with baseball bats";

    for(File f : filesToProcess){
      DocumentBean result = dpPool.process("general", f);
      System.out.println(f.getName());
      holdem.add(result);
    }

    dpPool.cleanup();
    dpPool = null;
   // dump(result);

  }

  public static void dump(DocumentBean doc) {
    System.out.println(doc.getContent().toString());
  }
}
