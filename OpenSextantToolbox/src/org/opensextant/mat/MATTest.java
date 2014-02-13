package org.opensextant.mat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.opensextant.toolbox.DocumentFactory;

public class MATTest {

  private MATTest() {

  }

  /**
   * @param args
   */
  public static void main(String[] args) {

    String jsonFilePath = args[0];

    String jsonString = "";

    try {
      jsonString = FileUtils.readFileToString(new File(jsonFilePath), "UTF-8");
    } catch (IOException e) {
      System.err.println("Couldnt read the json string in from file:" + jsonFilePath + " Error was" + e.getMessage());
    }

    MATDocument mat1 = DocumentFactory.matfromJSONString(jsonString);

    List<Aset> assets = mat1.getAsets();

    for (Aset a : assets) {
      System.out.println("---" + a.getType() + "---");
      List<Attr> attrs = a.getAttrs();

      System.out.print("start" + "\t" + "end");
      for (Attr at : attrs) {
        System.out.print("\t" + at.getName() + "\t");
      }
      System.out.println();
      List<Annot> annos = a.getAnnots();
      for (Annot an : annos) {
        System.out.print(an.getStart() + "\t" + an.getEnd());
        for (Object o : an.getAttributeValues()) {
          System.out.print("\t" + o.toString());
        }
        System.out.println();
      }
    }

    String jsonStringBack = DocumentFactory.jsonfromMATDocument(mat1);

    System.out.println(jsonStringBack);

  }
}
