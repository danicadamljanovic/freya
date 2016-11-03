package org.freya.similarity;

import static java.net.URLDecoder.decode;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.OntologyElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;

/**
 * This class is just to illustrate usage of wordnet...
 * 
 * @author danica
 */
@Component
public class WordnetInvoker {
    static Log logger = LogFactory.getLog(WordnetInvoker.class);
    private final WordNetDatabase wordnetDatabase;
    @Autowired
    public WordnetInvoker(@Value("${org.freya.wordnet.database.dir}") final Resource wordnetDir) {
        wordnetDatabase = load(wordnetDir);
    }

    /**
     * 
     * @return
     */
    public Set<String> findVerbs() {
        final Set<String> related = new HashSet<String>();
        for (final Synset synset : wordnetDatabase.getSynsets("eat", SynsetType.VERB)) {

            final VerbSynset verbSynset = (VerbSynset) synset;
            related.addAll(asList(verbSynset.getWordForms()));
            related.addAll(wordsFrom(verbSynset.getHypernyms()));
            // related.addAll(wordsFrom(verbSynset.getSentenceFrames()));
            // related.addAll(wordsFrom(verbSynset.getPartMeronyms()));
        }
        return related;
    }

    public Set<String> termsRelatedTo(final String term) {
        final Set<String> related = new HashSet<String>();

        for (final Synset synset : wordnetDatabase.getSynsets(term, SynsetType.NOUN)) {
            final NounSynset nounSynset = (NounSynset) synset;
            related.addAll(asList(nounSynset.getWordForms()));
            related.addAll(wordsFrom(nounSynset.getHypernyms()));
            related.addAll(wordsFrom(nounSynset.getPartHolonyms()));
            related.addAll(wordsFrom(nounSynset.getPartMeronyms()));
        }

        return related;
    }

    private static WordNetDatabase load(final Resource wordnetDir) {
        try {
            System.setProperty("wordnet.database.dir",
                            decode(wordnetDir.getURL().getFile(), "UTF-8"));

            return WordNetDatabase.getFileInstance();
        } catch (final Exception exception) {
            try {
                System.setProperty("wordnet.database.dir", wordnetDir.getFilename());
                return WordNetDatabase.getFileInstance();
            } catch (final Exception cause) {
                throw new RuntimeException(cause);
            }
        }
    }

    private static Set<String> wordsFrom(final Synset[] synsets) {
        final Set<String> words = new HashSet<String>();

        for (final Synset synset : synsets) {
            for (final String string : synset.getWordForms()) {
                words.add(string);
            }
        }

        return words;
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.setProperty(
				"wordnet.database.dir",
				"/Users/danica/projects/freya/freya/freya-annotate/src/main/resources/WordNet-3.0/dict/");
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		// database.getSynsets(wordForm);
		// retrieve synsets from the database
		// Synset[] nounFlies = database.getSynsets("split", SynsetType.NOUN);
		// Synset[] allFlies = database.getSynsets("split");
		// System.out.println("Synsets for split:");
		// for (Synset s : allFlies) {
		// System.out.println(s.toString());
		//
		// }
		String a[] = { "music", "art", "school" };

		int j = 0;
		while (j < 2) {

			NounSynset nounSynset;
			NounSynset[] hyponyms;

			Synset[] synsets = database.getSynsets(a[j], SynsetType.NOUN);
			System.out.println("*********************************************");
			for (int i = 0; i < synsets.length; i++) {
				nounSynset = (NounSynset) (synsets[i]);
				hyponyms = nounSynset.getHyponyms();

				System.err.println(nounSynset.getWordForms()[0] + ": "
						+ nounSynset.getDefinition() + ") has "
						+ hyponyms.length + " hyponyms:");

				for (NounSynset hyponym : hyponyms) {
					System.out.println(hyponym.getWordForms()[0]);
				}
			}
			j++;
		}
		System.out.println("*********************************************");

	}

    /**
     * @param text
     * @param suggestions
     * @return
     */
    public float getSynonymWeight(String text, List<OntologyElement> suggestions) {
        // WordnetInvoker wordnetI = new WordnetInvoker();
        // WordNetDatabase database = WordNetDatabase.getFileInstance();
        Synset[] synonyms = wordnetDatabase.getSynsets(text);
        logger.debug("Wordnet found " + synonyms.length
                        + " synonyms, listing only the first one &&&&&&&&&&&&&&&&&&&");
        int size = 0;
        if (synonyms != null && synonyms.length > 0) {
            if (synonyms.length < 1)
                size = synonyms.length;
            else
                size = 1;
        } else {
            logger.info("No synonyms found for " + text);
        }
        for (int i = 0; i < size; i++) {
            Synset s = synonyms[i];
            String forms = "";
            for (String word : s.getWordForms()) {
                forms = forms + word;
                forms = forms + ",";
            }
            logger.info(i + 1 + ". " + forms);
        }
        float sim = 0;
        return sim;
    }

    /**
     * 
     * @param text
     * @param numberOfSynonyms
     * @return
     */
    public Set<String> getSynonyms(String text, int numberOfSynonyms) {
        Set<String> toReturn = new HashSet<String>();
        Synset[] synonyms = wordnetDatabase.getSynsets(text);
        logger.debug(synonyms.length + " syns for " + text);

        int size = 0;
        if (synonyms != null && synonyms.length > 0) {
            if (synonyms.length < numberOfSynonyms)
                size = synonyms.length;
            else
                size = numberOfSynonyms;
        } else {
            // logger.debug("No synonyms found for " + text);
        }
        for (int i = 0; i < size; i++) {
            Synset s = synonyms[i];
            for (String word : s.getWordForms()) {
                if (!text.toLowerCase().equals(word.toLowerCase())) {
                    toReturn.add(word);
                    logger.debug("Adding syn:" + word);

                }
            }

        }

        return toReturn;
    }

    public Set<String> getSynonyms(String text, int numberOfSynonyms, String synType) {
        Set<String> toReturn = new HashSet<String>();
        Synset[] synonyms = wordnetDatabase.getSynsets(text);
        logger.debug(synonyms.length + " syns for " + text);

        int size = 0;
        if (synonyms != null && synonyms.length > 0) {
            if (synonyms.length < numberOfSynonyms)
                size = synonyms.length;
            else
                size = numberOfSynonyms;
        } else {
            // logger.debug("No synonyms found for " + text);
        }
        for (int i = 0; i < size; i++) {
            Synset s = synonyms[i];
            for (String word : s.getWordForms()) {
                if (!text.toLowerCase().equals(word.toLowerCase())) {
                    if (synType != null && synType.equals(s.getType().toString()))
                        toReturn.add(word);
                    logger.debug("Adding syn:" + word);
                }
            }
        }
        return toReturn;
    }
}
