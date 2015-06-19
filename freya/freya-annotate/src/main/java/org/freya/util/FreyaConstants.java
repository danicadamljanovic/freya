package org.freya.util;

public class FreyaConstants {
    
    public static final String SENTENCE_DELIMITER = "\n ";
    
    public static String QUESTION = "processedQuestion";
    public static String SEMANTIC_CONCEPTS = "semanticConcepts";

    public static String VARIABLE_E = "E";
    public static String VARIABLE_L = "L";
    public static String VARIABLE_P = "P";
    public static String VARIABLE_T = "T";

    public static String POC_TYPE = "POC";
    public static String OC_TYPE = "OC";

    public static String OWL_CLASS = "http://www.w3.org/2002/07/owl#Class";

    public static String SOLR_FIELD_ID = "id";
    public static String SOLR_FIELD_CLASS = "class_s";
    public static String SOLR_FIELD_INSTANCE = "inst_s";
    public static String SOLR_FIELD_PROPERTY = "pred_s";
    public static String SOLR_FIELD_STEMMED_CONTENT = "stem_en";
    /** see also: setting in solr schema.xml <copyField source="prec_s" dest="prec_lowercase"/> */
    public static String SOLR_FIELD_EXACT_CONTENT = "prec_s";
    public static String SOLR_FIELD_LOWERCASE_CONTENT = "prec_lowercase";


    public static String CLASS_FEATURE_LKB = "class";

    public static String INST_FEATURE_LKB = "inst";

    public static String PROPERTY_FEATURE_LKB = "pred";

    public static String FIELD_EXACT_CONTENT = "prec";

    public static String FIELD_EXACT_LOWERCASED_CONTENT = "low";

    public static String FIELD_STEMMED_CONTENT = "stem";

    public static String SCORE = "score";

    public static String MAX_FUNCTION = "max";

    public static String MIN_FUNCTION = "min";

    public static String SUM_FUNCTION = "sum";
    
    public static String AVG_FUNCTION = "avg";

    public static String NONE_NEIGHBOURING_ONTOLOGY_ELEMENT = "NONENEIGHBOURINGONTOLOGYELEMENT";

    public static String INSTANCE_WITHOUT_DIRECT_CLASS = "INSTANCEWITHOUTDIRECTCLASS";

    public static String NUMBER_OF_RESULTS = "numberOfResults";
    public static String TRIPLE_ELEMENTS = "tripleElements";
    public static String RESULTS_GRAPH = "resultsGraph";

    public static String ANSWER = "answer";



    public static String ELEMENTS = "elements";

    public static String TABLE = "table";
    public static String GRAPH = "graph";
    
    public static String SPARQL = "sparql";

    public static String PRECISE_SPARQL = "preciseSparql";

    public static String REPOSITORY_URL = "repositoryUrl";

    public static String REPOSITORY_ID = "repositoryId";
    // ///////////////////////////////////////
    // query specific constants ////////////
    // //////////////////////////////////////
    public static String NP_TAG_TREEBANK = "NP";
    public static String NN_TAG_TREEBANK = "NN";
    public static String NX_TAG_TREEBANK = "NX";
    public static String PRP_TAG_TREEBANK = "PRP";
    public static String EX_TAG_TREEBANK = "EX";
    public static String WHADVP_TAG_TREEBANK = "WHADVP";
    public static String WHADJP_TAG_TREEBANK = "WHADJP";
    public static String WHNP_TAG_TREEBANK = "WHNP";
    public static String WRB_TAG_TREEBANK = "WRB";
    public static String WP_TAG_TREEBANK = "WP";
    public static String RB_TAG_TREEBANK = "RB";
    /** JJ adjective, JJR comparative, JJS superlative */
    public static String JJ_TAG_TREEBANK = "JJ";
    /** VBN verb, past participle */
    public static String VBN_TAG_TREEBANK = "VBN";
    /** RBS adverb, superlative */
    public static String RBS_TAG_TREEBANK = "RBS";

    public static String ADJP_TAG_TREEBANK = "ADJP";
    public static String DT_TAG_TREEBANK = "DT";

    public static boolean DEBUG_MODE = false;

    public static String ANNOTATION_FEATURE_TREE = "Tree";

    public static String STANFORD_TREE = "StanfordTree";

    public static String RESULT_TYPE_GRAPH = "Graph";

    public static String RESULT_TYPE_TREE = "Tree";

    public static String RESULT_TYPE_STRING = "string";

    public static String ANNOTATION_TYPE_ONTORES = "OntoRes";

    public static String ANNOTATION_TYPE_ONTORESCHUNK = "OntoResChunk";

    /* used when grouping elements to indicate if it is a conjunction element */
    public static String CONJUNCTION = "and";

    /* used when grouping elements to indicate if it is a disjunction element */
    public static String DISJUNCTION = "or";

    /**
     * this constant is used to indicate wheather the keyword is from the gazetteer with this name
     */
    public static String LIST_COMMANDS = "listCommands";

    /**
     * Name of the common logger
     */
    public static String LOGGER_NAME = "CLOnE-QL-logger";

    public static String LOGGER_OUPUT_LEVEL = "2000";

    /* separator used during formatting of results */
    public static String TRIPLES_SEPARATOR = " --> ";

    /*
     * flag indicating the output of the result, in this case it is refering to the resource names
     */
    public static String SHOWING_TRIPLES_WITH_RESOURCE_NAMES = "showingTriplesWithResourceNames";

    /*
     * flag indicating the output of the result, in this case it is refering to the labels
     */
    public static String SHOWING_TRIPLES_WITH_LABELS = "showingTriplesWithLabels";

    public static String REGEX_GROUPS_SEPARATED_BY_AND = "i\\d+-and-i\\d+(-and-i\\d+)*";

    public static String REGEX_GROUPS_SEPARATED_BY_OR = "i\\d+-or-i\\d+(-or-i\\d+)*";

    public static String GROUP_PREFIX = "gs:";

    public static String GROUP_SUFFIX = ":ge";

    public static String REGEX_PPP = "[i,c,d,g]\\d+-([k,o,p]\\d+-)*[i,c,d,g]\\d+";

    public static String REGEX_FIND_JOKER = "[i,c,d,g]\\d+-[i,c,d,g]\\d+";

    public static String REGEX_FIND_CLASS_JOKER = "[p]\\d+-[p]\\d+";

    public static String POTENTIAL_PROPERTY_POSITION = "-r";

    public static String EXACT_PROPERTY = "-ep";

    public static String POTENTIAL_RELATED_ELEMENTS = ":";

    public static String MAX_SIMILARITY_VALUE = "1";

    /* three types of weights - to show the importance of every type of score */
    public static double SIMILARITY_SCORE_WEIGHT = 3.0;
    public static double SPECIFICITY_SCORE_WEIGHT = 1.0;
    public static double DISTANCE_SCORE_WEIGHT = 1.0;

    public static String SELECT = "select distinct";
    
    public static String SELECT_SUM ="select (SUM(xsd:decimal(";
    
    public static String SELECT_AVG ="select (AVG(xsd:decimal(";

    public static String WHERE = "where";

    public static String FROM = "from";

    public static String INVERSE_PROPERTY = "[inverseProperty]";

    public static String JOKER = "joker";

    public static String LITERAL_VALUE_CONNECTOR = "is";

    // these are used as annotation type names in order to transwer the results
    // from the gate pipeline elsewhere
    public static String QUERY = "query";
    public static String INTERPRETATIONS_LIST = "interpretationsList";
    public static String SELECTED_INTERPRETATION = "selectedInterpretation";
    public static String INTERPRETATION_RESULTS = "interpretationResults";
    public static String QUERY_INTERPRETATIONS = "queryInterpretations";
    public static String MAP_OF_LABELS = "mapOfLabels";

    public static double LEVENSHTEIN_THRESHOLD = 0.1;
    public static double MONGE_THRESHOLD = 0.5;
    public static double MAX_MONGE_THRESHOLD = 1.0;

    public static double REINFORCEMENT_REWARD = 1.0;
    public static long REINFORCEMENT_NULL_STATE = 0;
    public static double REINFORCEMENT_NEGATIVE_REWARD = -1.0;

    public static String CLARIFICATION_OPTIONS_NONE = "none";

    public static String CLARIFIED_INTERPRETATION_DETAILS = "clarifiedInterpretationDetails";

    public static String CIPIN_SEPARATOR = ",";

    /* priority over ontology annotations */
    public static int MAIN_SUBJECT_PRIORITY_MAX = 100;

    public static int MAIN_SUBJECT_PRIORITY_MIN = 0;


}
