OpenSextant Toolbox 2.0 (prebuilt distro)

To use the OpenSextantToolbox 2.0 prebuilt distro, you will need
  a. Java 1.6 or later
  b. ant (to run examples)


unzip opensextant-toolbox-2.0.zip. This should produce the "opensextant-toolbox-2.0" directory = <TOOLBOX_HOME>

to run the examples
------------------
 cd <TOOLBOX_HOME>
 ant -f examples.xml
 
 This will run the 6 example programs:
 1) Geotagger - finds geographic coordinates and named places in text
 2) General Purpose extractor - finds geographic coordinates and named places plus a whole bunch more types in text
 3) Solr Matcher - shows how to use the Solr matcher which we use to identify place name candidates with our Solr based gazetteer
 4) Solr Searcher - shows how to lookup names in our Solr based gazetteer
 5) Date Regex matcher - shows how to use our regex-based finder component to find dates in text
 6) Geocoord Regex Matcher -shows how to use our regex-based finder component to find geographic coordinates in text
 
 The GeoTagger and General Purpose Extractor examples show how to use the two complete tagging processes. The other examples show how
 to use some of the important subcomponents of those processes (in case you want to use them by themselves, test/diagnose the pipelines behavior
 or just curious)
 
 Note: if you run the tests using a JRE (not a JDK) you will see a warning like "Unable to locate tools.jar ...". This can be ignored.
 
to write your own app using the geotagger or general purpose extractor
----------------------------------------------------------------------
First, take a look at the GeotaggerExample and GeneralPurposeTaggerExample examples in <TOOLBOX_HOME>/examples and the examples.xml ant file (which shows how to calls them.
 You will see that both examples use the same basic flow consisting of four major steps
 1) initialize GATE and the geotagger or general purpose extractor (this only has to be done once per session)

 then for each document to be processed
 
 2) create a GATE document using the GATE Factory, passing in a string, file or URL
 3) add the document to the corpus and then call execute on the application 
 4) get the results (the annotations) from the document and do something with them
 
 These examples just print out some basic info about all annotations found but they demonstrate the major steps needed to call the tagger
 and get the results.
 The examples.xml ant file show the input parameters needed to run these examples (location of GATE,Solr gazetteer etc) 