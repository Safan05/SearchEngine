package org.example;
import java.util.*;
import java.util.regex.*;

public class TextProcessor {
    private static final Set<String> stopWords = new HashSet<>(Arrays.asList(
            // Articles
            "a", "an", "the",

            // Pronouns
            "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them",
            "my", "your", "his", "its", "our", "their", "mine", "yours", "hers", "ours",
            "theirs", "myself", "yourself", "himself", "herself", "itself", "ourselves",
            "yourselves", "themselves", "who", "whom", "whose", "what", "which", "that",
            "this", "these", "those", "anybody", "anyone", "anything", "everybody",
            "everyone", "everything", "nobody", "noone", "nothing", "somebody", "someone",
            "something", "whosoever", "whomsoever", "whatever", "whichever", "whoever",

            // Prepositions
            "about", "above", "across", "after", "against", "along", "among", "around",
            "as", "at", "before", "behind", "below", "beneath", "beside", "between",
            "beyond", "by", "despite", "down", "during", "except", "for", "from", "in",
            "inside", "into", "like", "near", "of", "off", "on", "onto", "out", "over",
            "past", "since", "through", "throughout", "till", "to", "toward", "towards",
            "under", "underneath", "until", "unto", "up", "upon", "via", "with", "within",
            "without", "aboard", "amid", "amidst", "amongst", "apropos", "astride", "atop",
            "barring", "besides", "circa", "concerning", "considering", "downwards",
            "excepting", "excluding", "following", "hence", "including", "insofar", "mid",
            "midst", "minus", "nigh", "notwithstanding", "o'er", "opposite", "outwards",
            "outside", "overseas", "pending", "per", "plus", "regarding", "round", "save",
            "saving", "thru", "twixt", "underfoot", "unlike", "versus", "vis-a-vis",

            // Conjunctions
            "and", "but", "or", "nor", "for", "yet", "so", "although", "because", "since",
            "unless", "while", "whereas", "if", "though", "whilst", "either", "neither",
            "lest", "provided", "providing", "supposing", "albeit", "as", "ere", "once",
            "than", "that", "till", "whenever", "wherever",

            // Common verbs (to be, to have, modals, auxiliaries)
            "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "having", "do", "does", "did", "doing", "shall", "should", "will", "would",
            "may", "might", "must", "can", "could", "ought", "seem", "seems", "seemed",
            "seeming", "get", "gets", "got", "gotten", "become", "became", "becoming",
            "come", "came", "coming", "go", "went", "gone", "going", "keep", "keeps",
            "kept", "make", "made", "making", "say", "said", "saying", "take", "took",
            "taken", "taking",

            // Contractions
            "ain't", "aren't", "can't", "cannot", "couldn't", "didn't", "doesn't", "don't",
            "hadn't", "hasn't", "haven't", "he'd", "he'll", "he's", "how'd", "how'll",
            "how's", "i'd", "i'll", "i'm", "i've", "isn't", "it's", "let's", "mightn't",
            "mustn't", "shan't", "she'd", "she'll", "she's", "shouldn't", "that's",
            "there's", "they'd", "they'll", "they're", "they've", "wasn't", "we'd",
            "we'll", "we're", "we've", "weren't", "what'll", "what're", "what's",
            "when's", "where'd", "where's", "who'd", "who'll", "who's", "why'd", "why's",
            "won't", "wouldn't", "you'd", "you'll", "you're", "you've", "y'all", "y'all'd",
            "y'all're", "y'all've",

            // Adverbs and particles
            "again", "already", "also", "always", "anyway", "anyways", "anywhere", "aside",
            "back", "else", "elsewhere", "even", "ever", "everywhere", "far", "further",
            "hence", "here", "hereafter", "hereby", "herein", "hereupon", "however",
            "indeed", "instead", "just", "maybe", "merely", "more", "moreover", "most",
            "mostly", "never", "nevertheless", "now", "nowhere", "often", "once", "only",
            "otherwise", "perhaps", "rather", "seldom", "sometimes", "somewhat", "soon",
            "still", "then", "thence", "there", "thereafter", "thereby", "therefore",
            "therein", "thereupon", "thus", "together", "too", "very", "when", "whence",
            "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon",
            "wherever", "why", "almost", "altogether", "awfully", "basically", "completely",
            "entirely", "exactly", "fairly", "generally", "hardly", "highly", "largely",
            "nearly", "partly", "perfectly", "pretty", "probably", "quite", "really",
            "simply", "surely", "totally", "truly", "usually",

            // Determiners and quantifiers
            "all", "another", "any", "both", "each", "either", "every", "few", "many",
            "most", "neither", "none", "no", "other", "others", "several", "some", "such",
            "certain", "enough", "half", "less", "little", "much", "plenty", "whole",

            // Other common function words
            "not", "same", "own", "re", "via", "viz", "vs", "etc", "eg", "ie", "ma'am",
            "mr", "mrs", "ms", "miss", "sir", "thing", "things", "stuff", "lot", "lots",
            "bit", "bits", "piece", "pieces", "part", "parts", "place", "places", "way",
            "ways", "time", "times",

            // Archaic and dialectal forms
            "thou", "thee", "thy", "thine", "ye", "art", "hast", "hath", "dost", "doth",
            "wert", "shalt", "wilt", "mayst", "mightst", "canst", "couldst", "wouldst",
            "shouldst", "twas", "tis", "twixt",

            // Miscellaneous high-frequency words
            "able", "abroad", "according", "accordingly", "actual", "actually", "afore",
            "afterwards", "against", "alongside", "always", "apart", "approximately",
            "around", "away", "behind", "below", "beside", "besides", "between", "beyond",
            "briefly", "certainly", "clearly", "commonly", "constantly", "continually",
            "currently", "definitely", "directly", "easily", "effectively", "entirely",
            "especially", "essentially", "eventually", "evidently", "exactly", "finally",
            "frequently", "fully", "generally", "gradually", "greatly", "henceforth",
            "hither", "immediately", "increasingly", "initially", "instantly", "lately",
            "likely", "literally", "mainly", "merely", "naturally", "normally", "obviously",
            "occasionally", "oft", "onward", "onwards", "originally", "partially",
            "particularly", "permanently", "possibly", "potentially", "presently",
            "primarily", "promptly", "quickly", "rapidly", "rarely", "readily", "regularly",
            "relatively", "repeatedly", "respectively", "roughly", "seemingly", "shortly",
            "significantly", "similarly", "slightly", "slowly", "specifically", "strongly",
            "subsequently", "successfully", "suddenly", "sufficiently", "surely",
            "temporarily", "thither", "throughout", "typically", "ultimately", "undoubtedly",
            "unfortunately", "usually", "vastly", "virtually", "widely"
    ));

    public static String normalize(String text) {
        text = text.toLowerCase();
        text = text.replaceAll("[^a-z\\s]", ""); // remove punctuation
        StringBuilder builder = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!stopWords.contains(word) && word.length() > 2) {
                builder.append(stem(word)).append(" ");
            }
        }
        return builder.toString().trim();
    }

    // Basic stemming
    public static String stem(String word) {
        if (word.endsWith("ing") || word.endsWith("ed")) {
            return word.substring(0, word.length() - 3);
        }
        return word;
    }
}
