##OpenSextantToolbox

[![Build Status](https://travis-ci.org/OpenSextant/OpenSextantToolbox.svg?branch=master)](https://travis-ci.org/OpenSextant/OpenSextantToolbox)

A GATE based geotagger

Based on the GATE open source text processing framework (http://gate.ac.uk), the OpenSextant Toolbox includes a set 
of GATE processing resources (plugins) which are assembled together to form a document processing pipeline which extracts geospatial information: geographic coordinates and named places. The OpenSextant Toolbox builds upon other elements of the OpenSextant project such as OpenSextant Gazetteer and SolrTextTagger projects.

###Get the latest release
  *Coming Soon*



###Building OpenSextant (must use *Java 8*)

* **Check it out** 

  Get it from Github 
   
* **Config**

  copy build.local.properties to build.properties and set proxy host and port if behind a firewall

* **To build a runnable release**

  `ant`  

  The release package will appear in the base directory of the OpenSextantToolbox project as opensextant-toolbox-VERSION.zip


* **To build a release and then run all the examples**

  `ant test` 

NOTE: Since loading the gazetteer data into solr can take a while, for testing/impatience you can use a small version of gazetteer which is much faster to load than the full gazetteer. Change the  value of "csv.gaz" in OpenSextantToolbox's build.xml to point to either the full or small gazetteer.  This small gazetteer contains countries, national capaitals, province/states and some big cities.  Although it represents less than 1% of the full gazetteer, it actually produces decent results on generic news-like documents.
  

### Using OpenSextant

The OpenSextant capabilities can be used in three ways:
 
1. Integrated into your own Java code  
See the examples in `org.opensextant.examples` package in the OpenSextantToolbox project. There are examples of how to setup and send documents to the geotagger and general purpose entity extractor pipelines. In addition there are examples for some of the major internal components that these processing pipelines use, such as:
  * how to tag documents using only the gazetter
  * how to lookup places names in the gazetteer
  * how to tag documents with the regular expression matcher component.

1. A REST service  
  The release includes a simple REST servive that provides access to the major capabilities.
  To start the REST services, run `scripts/start.bat` or `start.sh` This will start the services, using the configuration defined in `etc/service-config.properties`. See `scripts/README_server.txt` for more config details as well as examples on how to call these REST services. There are also some instructions to install the services as Linux service.
  
1. Used within the GATE Developer design and test tool   
  The GATE framework (on which much of OpenSextant is built) provides a design and test tool called GATE Developer. This is useful to debug a processing pipeline, to test the results of an extraction against a standard data set or just to see the innards of an extraction process. To use GATE Developer, first download and install the full GATE distro (http://gate.ac.uk/download) (The GATE used in OpenSextant build is the runtime part of GATE and doesn't include GATE Developer and a large number of plugins for all kinds of language processing). Install this somewhere handy. In the OpenSextantToolbox project, make sure you set the value of "gate.home" in the build.properties file to where you just installed GATE. Then do "ant toolbox-deploy" which will copy the OpenSextant plugin into the GATE distribution. Edit the GATE Developer startup config ("gate.l4j.ini" in the top level of the GATE distro you just installed) to tell GATE about the extra jars files it needs for OpenSextant and the location of the solr gazeteer contained in the release, by adding the following lines:  
  `-Dgate.class.path="<WHERE.YOU.INSTALLED.GATE\plugins\OpenSextantToolbox\lib_extra\*"`  
  `-Dsolr.home="<WHERE.YOU.UNZIPPED.THE.RELEASE>\solr"`
  
 Having done all of the above, you can open one of the OpenSextant pipelines (called GAPP (GATE Application) files in GATE nomenclature) in GATE Developer via `File->"Restore application from File` and selecting one of the OpenSextant GAPP files located in the `LanguageResources/GAPPs` directory. That will load up the GAPP file, exposing all of the configuration and details of that pipeline. Read the GATE documentation to see what you can do from there (run, test modify ...)
