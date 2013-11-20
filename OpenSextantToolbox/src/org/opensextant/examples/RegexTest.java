package org.opensextant.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.opensextant.regex.RegexAnnotation;
import org.opensextant.regex.RegexMatcher;

/**
 * A simple example of using the RegexMatcher which is used to tag text based on regular expressions defined in a
 * config file. This example reads a tab separated file containing text to be tagged. That file should be in the form:
 * EntityType _TAB_ POS/NEG _TAB_ TestPhrase where <ol> <li>EntityType is the type of entity</li> <li>POS/NEG indicates whether this is a
 * positive (should be tagged) or negative example (should not be tagged)</li> <li>TestPhrase is the text which should be
 * examined</li> <p>
 * See LanguageResources/TestData/RegexTestData for example test inputs<br>
 * See LanguageResources/resources/patterns for regex pattern definitions
 */
public class RegexTest {

  private RegexTest() {
  }


  public static void main(String[] args) {

    // file containing the tab separated test data
    String testFileName = args[0];
    // the file containing the regex definintions
    String patternFileName = args[1];
    // file into which we will write the results
    String resultFileName = args[2];

    File testFile = new File(testFileName);
    URL patternFile = null;
    BufferedWriter resWriter = null;

    // setup the output
    File resFile = new File(resultFileName);
    try {
      resWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resFile), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      System.err.println("Couldnt write to " + resFile.getName() + ":" + e.getMessage());
      return;
    } catch (FileNotFoundException e) {
      System.err.println("Couldnt write to " + resFile.getName() + ":" + e.getMessage());
      return;
    }

    // write the results header
    try {
      resWriter.write("Entity Type\tPos\\Neg\tTest Input\tScore\tCount\tTypes Found\tRules Matched\tAnnotations Found");
      resWriter.newLine();
    } catch (IOException e) {
      System.err.println("Couldnt write to " + resFile.getName() + ":" + e.getMessage());
    }

    // get the pattern file as a URL
    try {
      patternFile = new File(patternFileName).toURI().toURL();
    } catch (MalformedURLException e) {
      System.err.println("Couldn't use pattern file " + patternFileName + ":" + e.getMessage());
    }

    // initialize the regex matcher
    RegexMatcher reger = new RegexMatcher(patternFile);
    System.out.println("Loaded " + reger.getRules().size() + " rules " + " for types " + reger.getTypes());
    System.out.println("Writing results to " + resFile.getAbsolutePath());

    // loop over the lines of the test file
    LineIterator iter = null;
    try {
      iter = FileUtils.lineIterator(testFile, "UTF-8");
    } catch (IOException e) {
      System.err.println("Couldnt read from " + testFile.getName() + ":" + e.getMessage());
    }

    int lineCount = 0;
    while (iter.hasNext()) {
      // get next line
      String line = iter.next();

      // skip comments and blank lines
      if (line == null || line.startsWith("#") || line.trim().length() == 0) {
        continue;
      }

      lineCount++;

      // get the fields of the line
      String[] pieces = line.split("[\t\\s]+", 3);
      String entityType = pieces[0];
      String posOrNeg = pieces[1];
      String testText = pieces[2];

      // send the test text to regex matcher
      List<RegexAnnotation> annos = reger.match(testText);
      // examine the results and return a line to be sent to the results file
      String results = score(entityType, posOrNeg, annos);
      try {
        // write the original line and the results to the results file
        resWriter.write(line + "\t" + results);
        resWriter.newLine();
      } catch (IOException e) {
        System.err.println("Couldn't write to " + resFile.getName() + ":" + e.getMessage());
      }

    }

    iter.close();

    System.out.println("Tagged and scored  " + lineCount + " test lines");

    // cleanup
    try {
      resWriter.flush();
      resWriter.close();
    } catch (IOException e) {
      System.err.println("Couldn't close " + resFile.getName() + ":" + e.getMessage());
    }

  }

  public static String score(String correctType, String posOrNeg, List<RegexAnnotation> annos) {

    String assessment = "??";
    int annoCount = annos.size();
    Set<String> typesFound = new TreeSet<String>();
    Set<String> rulesFired = new TreeSet<String>();
    String tmpRes = "";

    for (RegexAnnotation a : annos) {
      typesFound.add(a.getType());
      rulesFired.add(a.getRule());
      tmpRes += (a.toString() + ",");
    }

    if (posOrNeg.equalsIgnoreCase("POS")) { // if POS example
      // perfect result
      if (typesFound.contains(correctType) && annoCount == 1) {
        assessment = "TP";
      }

      // got it plus some other type(s)
      if (typesFound.contains(correctType) && typesFound.size() > 1) {
        assessment = "TP+FP";
      }

      // got it but more than once
      if (typesFound.contains(correctType) && typesFound.size() == 1 && annoCount > 1) {
        assessment = "TP+DUPE";
      }

      // got wrong type
      if (!typesFound.contains(correctType) && annoCount > 0) {
        assessment = "FN+FP";
      }

      // got nothing
      if (annoCount == 0) {
        assessment = "FN";
      }

    } else { // NEG examples
      // perfect result (doesnt matter what else is found)
      if (!typesFound.contains(correctType)) {
        assessment = "TN";
      } else {
        assessment = "FP";
      }

    }

    String results = assessment + "\t" + annoCount + "\t" + typesFound.toString() + "\t" + rulesFired.toString() + "\t"
        + tmpRes;
    return results;
  }

}
