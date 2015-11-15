# FREyA
FREyA - a Natural Language Interface for Querying Ontologies

FREyA is an interactive Natural Language Interface for querying ontologies which combines usability enhancement methods such as feedback and clarification dialogs in order to:
1) improve recall by generating the dialog and enriching the domain lexicon from the user's vocabulary, whenever an "unknown" term appears in a question
2) improve precision by resolving ambiguities more effectively through the dialog. The suggestions shown to the user are found through ontology reasoning and are initially ranked using the combination of string similarity and synonym detection. The system then learns from the user's selections, and improves its performance over time.

FREyA Web Site:
https://sites.google.com/site/naturallanguageinterfaces/freya


 Install FREyA
--------------------------------------------------------------------------------
STEP 1: SET UP RDF REPOSITORY

You can use sesame workbench to set up a Sesame SPARQL endpoint or you could use OWLIM:

[Setting up OWLIM repository using Sesame Workbench (http://researchsemantics.blogspot.co.uk/2012/03/set-up-your-own-sparql-endpoint-with.html)]

Download Sesame version 4.0 from http://rdf4j.org/

Copy the two war files from war directory into your tomcat's webapp directory

Open http://localhost:8080/openrdf-workbench in your browser and create a new repository.

For example, you can specify id: 'mooney' and type of repository as 'Native Java Store RDF Schema aand Direct Type Hierarchy'.

Leave everything else as default.

When you created a repository, click 'Add' under 'Modify' and upload your ontology. 

You can also use mooney ontology from freya-annotate/src/main/resources/ontologies/mooney folder.

STEP 2: SET UP SOLR REPOSITORY

Download Solr 4.6 version: http://archive.apache.org/dist/lucene/solr/4.6.0/

unpack SOLR and go to example dir

Copy conf file from freya-annotate/src/main/resources/solr into relevant dir -> example/solr/collection1/conf 

run Solr:

from the example dir run:

java -jar start.jar

this will start solr on the default port: 8983


STEP 3: INSTALL FREYA

Install FREyA
--------------------------------------------------------------------------------
STEP 1: Check out the FREyA code

cd to the dir where you want to check out the freya project e.g. 

cd ~/projects

git clone https://github.com/danicadamljanovic/freya freya

The default settings for the rdf repository is 'http://localhost:8080/openrdf-sesame' as repositoryUrl, and 'mooney' as id.
If you wish to change that you will need to do so before building freya: update your repositoryURL and repositoryId in
 src/main/resources/META-INF/spring/freya.properties file.

STEP 2: mvn clean install -DskipTests will create war file in freya-annotate/target directory and skip running all tests
STEP 3: Copy war file into your tomcat webapps folder e.g. 
cp freya-annotate/target/freya.war /Applications/apache-tomcat-8.0.28/webapps/
STEP 4: Start tomcat e.g. from tomcat's bin directory do sh ./catalina.sh run
STEP 5: Open the home page: http://localhost:8080/freya
STEP 6: Click 'Reindex' (or point your browser to http://localhost:8080/freya/service/solr/reindex)


Reindexing SOLR:
----------------------------------------
If for any reason you want to wipe out the SORL index and build it again, the best way to do this is as follows:

1. Start up your solr
2. Start up Freya platform
3. Call **Reindex** from the Freya home page (or call http://localhost:8080/freya/service/solr/reindex from your browser)

By clicking the **reindex** link, the existing index will be automatically removed, and you will be able to see the reindex processing in a new browser window.

In case that you have problems with solr, you can go to it's admin console and view the index. To clean the index completely:
1. In the browser call this command:
http://localhost:8983/solr/update?stream.body=%3Cdelete%3E%3Cquery%3E*:*%3C/query%3E%3C/delete%3E
2. Then this command:
-http://localhost:8983/solr/update?stream.body=%3Ccommit/%3E


What types of natural language queries are supported by FREyA?
--------------------------------------------------------------------------------
Factual questions, e.g.:
-  List cities.
-  What is the capital of California?
-  What is the smallest city in California? (using minimum function on cityPopulation of City locatedIn California)
-  What is the largest city in California? (using maximum function on cityPopulation of City locatedIn California)
-  What is the total state area? (using sum function on stateArea)
-  What is the average population of the cities in california? (using avg function on cityPopulation of City locatedIn California)  


SPARQL example
----------------------------------------
NLP query: 

List cities.

SPARQL query:

prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

prefix xsd: <http://www.w3.org/2001/XMLSchema#>

select distinct ?c0 where {{{  ?c0  ?typeRelationc0 <http://www.mooney.net/geo#City> .  }}} 

LIMIT 10000


Uploading bulk ontologies using Freya:
----------------------------------------
use loadBulk service from FreyaService.

See an example in FreyaServiceTest.loadBulk;


Lucene instead of Solr (depricated)
----------------------------------------
FREya out of the box works with Solr. It is possible to use it with 
Lucene only, however that will require some code changes. Below notes 
are relevant if you decide to do that. This is not a recommended route.


How to set up FREyA to work with a new dataset (initialise the lucene index):
----------------------------------------
1. create empty owlim repository (ruleset=empty, so no inference)
2. create Lucene/SOLR index - basic
3. create another owlim repository with ruleset=rdfs
4. update Lucene index (connect to owlim-rdfs):
5. add subClasses
6. add properties
7. START FREyA