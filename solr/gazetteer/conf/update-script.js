/*
  This is a basic skeleton JavaScript update processor.

  In order for this to be executed, it must be properly wired into solrconfig.xml; by default it is commented out in
  the example solrconfig.xml and must be uncommented to be enabled.

  See http://wiki.apache.org/solr/ScriptUpdateProcessor for more details.
*/

function processAdd(cmd) {

  doc = cmd.solrDoc;  // org.apache.solr.common.SolrInputDocument

  // CREATE geo field from lat and lon fields 
  lat = doc.getFieldValue("lat");
  lon = doc.getFieldValue("lon");
  
  if (lat != null && lon != null)
    doc.setField("geo", lat+","+lon);

  // optionally load only selected gazetteer partitions
  // part =doc.getFieldValue("partition");
  // if(part != null && part != "Basic" ) 
  //	  return false;
 
}

function processDelete(cmd) {
  // no-op
}

function processMergeIndexes(cmd) {
  // no-op
}

function processCommit(cmd) {
  // no-op
}

function processRollback(cmd) {
  // no-op
}

function finish() {
  // no-op
}
