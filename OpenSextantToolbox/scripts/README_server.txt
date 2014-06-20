
To start the OpenSextant REST server
 1) unzip the prebuilt release somewhere handy (<OpenSextantHome>)
 
 2) (optional) if server is running behind a proxy and fetching content from outside the proxy, configure proxy:
 	edit <OpenSextantHome>/scripts/start.bat
 	uncomment and set values for proxyhost and proxyport
 
 3) (optional) configure number of extractors
 	 edit <OpenSextantHome>/etc/service-config.properties:
 	 change os.service.app.geo.poolsize to indicate how many geotagger extractors to start
 	 change os.service.app.general.poolsize to indicate how many general purpose extractors start 
 	 
 2) (Windows) double clink <OpenSextantHome>/scripts/start.bat
 
Two types of services available
 1) Extraction - process a document to extract places, people, organizations,...
    available at http://<hostname>:8182/opensextant/extract
 2) Lookups - query the geospatial gazetteer for place names by name, country and other attributes
	available at http://<hostname>:8182/opensextant/lookup
	
 Examples of using the OpenSextant REST server (replace "localhost" with actual hostname if calling from another machine)

------------ Example Extraction Calls ---------

-- get the list of available extractor types 
curl -X POST http://localhost:8182/opensextant/extract/

-- get the list of available results formats for the specified process
curl -X POST http://localhost:8182/opensextant/extract/geo/

-- 3 different ways to POST a text file to the "geo" extractor and return CSV format
-- simple POST
curl -X POST http://localhost:8182/opensextant/extract/geo/csv -d "@../testdata/ace.txt"
-- POST using a form
curl -X POST http://localhost:8182/opensextant/extract/geo/csv -F "infile=@../testdata/ace.txt"
-- POST using a form and specifying the file type of the uploaded file
curl -X POST http://localhost:8182/opensextant/extract/geo/csv -F "infile=@../testdata/ace.htm;type=text/html"

-- POST a file by sending in a URL where the file is located (URL string used must be URL safe (i.e. space,/.: etc replace by %-codes )
-- NOTE that the REST SERVER (not the machine making the request) must be able to fetch the URL
curl -X POST "http://localhost:8182/opensextant/extract/geo/csv/url/http%3A%2F%2Fwww.cnn.com%2F2014%2F06%2F20%2Fworld%2Feurope%2Fukraine-crisis%2Findex.html?hpt=wo_c2

--- send the data as part of the REST URL (probably not a good idea for large amounts of text) 
curl -X POST http://localhost:8182/opensextant/extract/geo/csv -d "We drove to Kabul, which is located at 1234N 01234W."
curl -X POST http://localhost:8182/opensextant/extract/geo/json -d "We drove to Kabul, which is located at 1234N 01234W."
curl -X POST http://localhost:8182/opensextant/extract/geo/xml -d "We drove to Kabul, which is located at 1234N 01234W."

 ------------Lookup Calls ---------
-- Lookup "Kabul" and get the results in two different return formats
curl -X GET http://localhost:8182/opensextant/lookup/json/Kabul
curl -X GET http://localhost:8182/opensextant/lookup/csv/Kabul

-- Lookup "Kabul" in Afghanistan and get the results in two different return formats
curl -X GET http://localhost:8182/opensextant/lookup/json/Kabul/AF
curl -X GET http://localhost:8182/opensextant/lookup/csv/Kabul/AF

-- General query in the form of <field>:<value> pairs (query must be URL-safe)
curl -X GET http://localhost:8182/opensextant/lookup/csv/query/name:Ka*%20AND%20cc:AF
-- query fields
name 			- place name
name_expanded 	- an expanded form of the name (only for name which are codes or abbreviations)
feat_class 		- the general type of feature. Valid values are A, P, L, H, V, S, T, R
feat_code		- specific type of feature. 
cc 				- ISO 2 character country codes
name_type 		- what type of name. Valid values are "name", "code" and "abbreviation"
geo 			- the point location (lat/lon) for the named place

