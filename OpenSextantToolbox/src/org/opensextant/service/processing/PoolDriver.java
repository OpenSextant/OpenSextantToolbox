package org.opensextant.service.processing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PoolDriver {

  /**
   * @param args
   */
  public static void main(String[] args) {

    Properties prop = null;
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

    String content = " They were attacked with baseball bats";
    DocumentBean result = dpPool.process("general", content);

    dump(result);

  }

  public static void dump(DocumentBean doc) {
    System.out.println(doc.getContent().toString());
  }
}
