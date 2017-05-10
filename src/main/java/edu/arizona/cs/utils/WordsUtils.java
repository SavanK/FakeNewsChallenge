package edu.arizona.cs.utils;

import edu.arizona.cs.data.DataRepo;
import edu.arizona.cs.data.Document;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.*;

/**
 * Created by savan on 4/29/17.
 */
public class WordsUtils {
    private static final long NUM_DOCS = 9615720;
    private static final String STOP_WORDS = "/stopwords.txt";
    private static final String CONTEXT_PRESERVING_STOP_WORDS = "/context_preserving_stopwords.txt";
    private static final String REFUTING_WORDS = "/refutingwords.txt";
    private static final String HEDGE_WORDS = "/hedgewords.txt";
    private static final String SUPPORTIVE_WORDS = "/supportivewords.txt";
    private static final String IDF_SCORES = "/gigawordDocFreq.txt";

    /**
     * Flip this flag to (en)disable lemmatization. In turn, stemming will be disabled.
     */
    public static final boolean LEMMATIZATION = false;

    /**
     * Flip this flag to (en)disable stemming. In turn, lemmatization will be disabled.
     */
    public static final boolean STEMMING = true;

    private static WordsUtils ourInstance = new WordsUtils();

    public static WordsUtils getInstance() {
        return ourInstance;
    }

    private Set<String> stopwordsSet;
    private Set<String> contextStopwordsSet;
    private Set<String> refutingSet;
    private Set<String> hedgeSet;
    private Set<String> supportiveSet;
    private HashMap<String, Integer> documentFrequencies;

    private IndexReader indexReader;
    private net.sf.extjwnl.dictionary.Dictionary dictionary;

    private WordsUtils() {
        stopwordsSet = new HashSet<String>();
        refutingSet = new HashSet<String>();
        hedgeSet = new HashSet<String>();
        supportiveSet = new HashSet<String>();
        documentFrequencies = new HashMap<String, Integer>();
        contextStopwordsSet = new HashSet<String>();

        try {
            BufferedReader stopReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(STOP_WORDS)));
            String line;
            while ((line = stopReader.readLine()) != null) {
                stopwordsSet.add(line);
            }
            stopReader.close();

            BufferedReader contextStopReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(CONTEXT_PRESERVING_STOP_WORDS)));
            while ((line = contextStopReader.readLine()) != null) {
                contextStopwordsSet.add(line);
            }
            contextStopReader.close();

            BufferedReader refuteReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(REFUTING_WORDS)));
            while ((line = refuteReader.readLine()) != null) {
                refutingSet.add(line);
            }

            refuteReader.close();

            BufferedReader hedgeReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(HEDGE_WORDS)));
            while ((line = hedgeReader.readLine()) != null) {
                hedgeSet.add(line);
            }

            hedgeReader.close();

            BufferedReader supportiveReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(SUPPORTIVE_WORDS)));
            while ((line = supportiveReader.readLine()) != null) {
                supportiveSet.add(line);
            }

            supportiveReader.close();

            BufferedReader idfReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(IDF_SCORES)));
            while ((line = idfReader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line);
                if(tokenizer.countTokens() == 2) {
                    documentFrequencies.put(tokenizer.nextToken(), Integer.parseInt(tokenizer.nextToken()));
                }
            }

            idfReader.close();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    public void setDictionary(net.sf.extjwnl.dictionary.Dictionary dictionary) {
        this.dictionary = dictionary;
        List<Set<String>> tempSynList = new ArrayList<Set<String>>();

        for(int i=0;i<3;i++) {
            for (String refuteWord : refutingSet) {
                try {
                    Set<String> synSet = getSynonymsFor(refuteWord);
                    if (!synSet.isEmpty()) {
                        tempSynList.add(synSet);
                    }
                } catch (JWNLException e) {
                    e.printStackTrace();
                }
            }

            for (Set<String> synSet : tempSynList) {
                refutingSet.addAll(synSet);
            }

            tempSynList.clear();
        }

        for(int i=0;i<3;i++) {
            for (String supportWord : supportiveSet) {
                try {
                    Set<String> synSet = getSynonymsFor(supportWord);
                    if (!synSet.isEmpty()) {
                        tempSynList.add(synSet);
                    }
                } catch (JWNLException e) {
                    e.printStackTrace();
                }
            }

            for (Set<String> synSet : tempSynList) {
                supportiveSet.addAll(synSet);
            }

            tempSynList.clear();
        }

        for(int i=0;i<1;i++) {
            for (String hedgetWord : hedgeSet) {
                try {
                    Set<String> synSet = getSynonymsFor(hedgetWord);
                    if (!synSet.isEmpty()) {
                        tempSynList.add(synSet);
                    }
                } catch (JWNLException e) {
                    e.printStackTrace();
                }
            }

            for (Set<String> synSet : tempSynList) {
                hedgeSet.addAll(synSet);
            }

            tempSynList.clear();
        }
    }

    private Set<String> getSynonymsFor(String lemma) throws JWNLException {
        Set<String> synonymSet = new HashSet<String>();
        IndexWordSet indexWordSet = dictionary.lookupAllIndexWords(lemma);
        for (IndexWord indexWord : indexWordSet.getIndexWordCollection()) {
            if (indexWord != null) {
                for (Synset synset : indexWord.getSenses()) {
                    for (Word word : synset.getWords()) {
                        synonymSet.add(word.getLemma());
                    }
                }
            }
        }
        return synonymSet;
    }

    public boolean isStopWord(String lemma) {
        return stopwordsSet.contains(lemma);
    }

    /**
     * Check if word is refuting
     * @param lemma
     * @return
     */
    public boolean isRefutingWord(String lemma) {
        return refutingSet.contains(lemma);
    }

    /**
     * Check if word is hedge
     * @param lemma
     * @return
     */
    public boolean isHedgeWord(String lemma) {
        return hedgeSet.contains(lemma);
    }

    /**
     * Check if word is supportive
     * @param lemma
     * @return
     */
    public boolean isSuportiveWord(String lemma) {
        return supportiveSet.contains(lemma);
    }

    public void setDocuments(DataRepo dataRepo) {
        try {
            Directory index = new RAMDirectory();
            StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, standardAnalyzer);
            IndexWriter writer = new IndexWriter(index, config);
            for (Document document: dataRepo.getDocuments()) {
                org.apache.lucene.document.Document docLucene = new org.apache.lucene.document.Document();
                docLucene.add(new TextField("Headline", document.getHeadline().getText(), Field.Store.YES));
                docLucene.add(new TextField("Body", dataRepo.getBodies().get(document.getBodyId()).getText(), Field.Store.YES));
                    writer.addDocument(docLucene);
            }
            writer.close();

            indexReader = DirectoryReader.open(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculate the idf score given word
     * @param word
     * @return
     */
    public double getIdfScores(String word) {
        double idfScore = 0;
        if(documentFrequencies.containsKey(word)) {
            int docFreq = 0;
            if(documentFrequencies.containsKey(word)) {
                documentFrequencies.get(word);
            }

            if(docFreq != 0) {
                idfScore = Math.log(NUM_DOCS / (double)docFreq);
            }
        } else {
            Term term1 = new Term("Body", word);
            Term term2 = new Term("Headline", word);
            int docFreq = 0;
            int docCount = 0;
            try {
                docFreq = indexReader.docFreq(term1) + indexReader.docFreq(term2);
                docCount = indexReader.getDocCount("Body") + indexReader.getDocCount("Headline");
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(docFreq != 0) {
                idfScore = Math.log(docCount / (double)docFreq);
            }
        }
        return idfScore;
    }

    /**
     * Check to see if is a context preserving stop word
     * @param lemma
     * @return
     */
    public boolean isContextPreservingStopWord(String lemma) {
        return contextStopwordsSet.contains(lemma);
    }

    /**
     * Tokenizeer does porter stemmer & stop word elimination
     * @param text
     * @return
     */
    public List<String> tokenize(String text) {
        PorterStemmer stemmer = new PorterStemmer();

        StandardTokenizer tokenizer = new StandardTokenizer(new StringReader(text));
        List<String> tokens = new ArrayList<String>();
        try {
            TokenStream tokenStream = new StopFilter(
                    new ASCIIFoldingFilter(new ClassicFilter(new LowerCaseFilter(tokenizer))),
                    EnglishAnalyzer.getDefaultStopSet());
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
                if(STEMMING) {
                    stemmer.setCurrent(token);
                    stemmer.stem();
                    token = stemmer.getCurrent();
                }

                tokens.add(token);
            }
            tokenStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tokens;
    }
}
