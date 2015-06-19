Schema.xml
==========

The schema.xml file contains all of the details about which fields SOLR
documents can contain, and how those fields should be dealt with when adding
documents to the index, or when querying those fields.

Please copy schema.xml file to override exiting schema of Freya SOLR server.

For example:
cp ~/projects/freya-branch/freya-annotate/src/main/resources/solr/conf/schema.xml solr/collection1/conf/

Start your solr sever:
cd solr-4.6.0/example/solr/collection1/conf/
java -jar -Djetty.port=8989 start.jar
