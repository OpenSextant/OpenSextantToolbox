OpenSextantToolbox
==================

[![Build Status](https://travis-ci.org/OpenSextant/OpenSextantToolbox.svg?branch=master)](https://travis-ci.org/OpenSextant/OpenSextantToolbox)

A GATE based geotagger

Based on the GATE open source text processing framework (http://gate.ac.uk), the OpenSextant Toolbox includes a set 
of GATE processing resources (plugins) which are assembled together to form a document processing pipeline which extracts geospatial information: geographic coordinates and named places. The OpenSextant Toolbox builds upon other elements of the OpenSextant project such as OpenSextant Gazetteer and SolrTextTagger projects.


Building OpenSextant from source
==================

Prerequisites

	1. Java 8 (Relase 2.1 is the last release to work with Java 7)
	2. the Kettle ETL software (see the Gazetter project for details)

1) Get the two projects from github

	Gazetteer (https://github.com/OpenSextant/Gazetteer.git)
	OpenSextantToolbox (https://github.com/OpenSextant/OpenSextantToolbox.git)

	if using Eclipse, you can "Import existing projects" (the projects include eclipse .project files)

	You should end up with four Eclipse projects:
	  "Gazetteer" - contains the processing to clean and transform the publicly available gazetteer data
	  "OpenSextantToolbox Master" - a simple driver for the OpenSextantToolbox and LanguageResources projects
	  "LanguageResources" - all the language data (vocabularies, rules, patterns etc) 
	  "OpenSextantToolbox" - the code for the language processing and entity extraction 

2) edit config to match environment

	In the OpenSextantToolbox and LanguageResources and Gazetter projects:
	 copy build.local.properties to build.properties and edit, setting proxy host and port
	 if behind a firewall
	 
	Gazetteer
	  see Gazetteer/README.txt for other config details. Basically, get the Kettle software,
	  edit the config files and run ant 

3) do the builds

	1) Build the gazetteer 
	   in gazetteer project - do  "ant publish-local"

	  This will:
	   a) fetch the gazetter data from the two sources (NGA and USGS)
	   b) run the data through the cleaning and transformation processes
	   c) zip and publish the resulting clean transformed data to local ivy repo
	  This artifact will get pulled from the local ivy repo when you build a release
	  of the whole OpenSextant project (see next step)

	NOTE: This step may take 45-90 minutes depending on the machine. It is not necessary to re-process
	the gazetteer data very often since it changes slowly. So unless you want to get the very latest,
	 wish to add your own gazetteer data or have modified the gazetteer processing, once should be enough.
 
	2) Build a release
	   in the LanguageResources project - do "ant publish-local"
	   in the OpenSextantToolbox project - do "ant release"

	 NOTE: Since loading the gazetteer data into solr can take a while, for testing/impatience you can use
	 a small version of gazetteer which is much faster to load than the full gazetteer. Change the 
	 value of "csv.gaz" in OpenSextantToolbox's build.xml to point to either the full or small gazetteer. 
	 This small gazetteer contains countries, national capaitals, province/states and some big cities.
	 Although it represents less than 1% of the full gazetteer, it actually produces decent results on
	 generic news-like documents.
  

	This will:
	 a) pull all the data together (gazetteer and language resources)
	 b) build all the code
	 c) create and populate a Solr data set with the gazetteer data
	 d) zip it all into a package, ready to deploy

	The release package will appear in the base directory of the OpenSextantToolbox project as
	 opensextant-toolbox-<version>-<release-data>.zip


4) To use OpenSextant

	The OpenSextant capabilities can be used in three ways:
	1) integrated into your own Java code
	    See the examples in org.opensextant.examples package in
	    OpenSextantToolbox project. There are examples on how to
	    setup and send documents to the geotagger and general purpose
	    entity extractor pipelines. In addition there are examples for
	    some of the major internal components that these processing
	    pipelines use, such as:
	     how to tag documents using the gazetter,
	     how to lookup places names in the gazetteer and
	     how to tag documents with the regular expression matcher component.

	2) called as a REST service from your own code
 	  To start the REST services, unzip the release package somewhere convenient.
 	  Double click <release-dir>/scripts/start.bat (Windows). (Need sh script)
 	  This will start the services, using the configuration defined in
 	  <release-dir>/etc/service-config.properties.
 	  See <release-dir>/scripts/README-service.txt for more details as well
 	  as examples on how to call these REST services.

	3) used within the GATE Developer design and test tool   
	   The GATE framework (on which much of OpenSextant is built)
	   provides a design and test tool called GATE Developer. This is
	   useful to debug a processing pipeline, to test the results
	   of an extraction against a standard data set or
	   just to see the innards of an extraction process. To use
	   GATE Developer, first downlaod and install the full GATE distro
	   (http://gate.ac.uk/download) (The GATE used in OpenSextant build
	   is the runtime part of GATE and doesn't include GATE Developer and
	   a large number of plugins for all kinds of language processing).
	   Install this somewhere handy. In the OpenSextantToolbox project,
	   make sure you set the value of "gate.home" in the build.properties
	   file to where youjust installed GATE. Then do "ant toolbox-deploy"
	   which will copy the OpenSextant plugin into the GATE distribution.
	   Edit the GATE Developer startup config ("gate.l4j.ini" in the top
	   level of the GATE distro you just installed)to tell GATE both about
	   the extra jars files it needs for OpenSextant and the location of the
	   solr gazeteer contained in the release,by adding the following lines:
	     -Dgate.class.path="<WHERE.YOU.INSTALLED.GATE\plugins\OpenSextantToolbox\lib_extra\*"
	     -Dsolr.home="<WHERE.YOU.UNZIPPED.THE.RELEASE>\solr"
       Having done all of the above, you can open one of the OpenSextant pipelines
       (called GAPP (GATE Application) files in GATE nomenclature)in GATE Developer
       via File->"Restore application from File" and selecting one of the OpenSextant
       GAPP files located in the <release-dir>/LanguageResources/GAPPs directory.
       That will load up the GAPP file, exposing all of the configuration and details
       of that pipeline. Read the GATE documentation to see what you can do from there
       (run, test modify ...)
