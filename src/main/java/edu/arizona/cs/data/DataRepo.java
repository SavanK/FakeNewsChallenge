package edu.arizona.cs.data;

import edu.arizona.cs.utils.PosUtils;
import edu.arizona.cs.utils.StopwordsUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.data.list.PointerTargetNode;
import net.sf.extjwnl.data.list.PointerTargetNodeList;
import net.sf.extjwnl.dictionary.Dictionary;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Created by savan on 4/28/17.
 */
public class DataRepo {

    private static final String BODY_ID = "Body ID";
    private static final String ARTICLE_BODY = "articleBody";
    private static final String HEADLINE = "Headline";
    private static final String STANCE = "Stance";

    private Map<Integer, Body> bodies;
    private List<Document> documents;
    private Dictionary dictionary;

    public DataRepo(Dictionary dictionary) {
        this.dictionary = dictionary;
        bodies = new HashMap<Integer, Body>();
        documents = new ArrayList<Document>();
    }

    public void readData(String trainStancesPath, String trainBodiesPath) throws FileNotFoundException {
        Reader in = new FileReader(trainBodiesPath);
        try {
            CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader());
            for (CSVRecord record : parser) {
                int bodyId = Integer.parseInt(record.get(BODY_ID));
                String bodyText = record.get(ARTICLE_BODY);

                Body body = new Body(bodyText);
                bodies.put(bodyId, body);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        in = new FileReader(trainStancesPath);
        try {
            CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader());
            for (CSVRecord record : parser) {
                int bodyId = Integer.parseInt(record.get(BODY_ID));
                String headlineStr = record.get(HEADLINE);
                String stanceStr = record.get(STANCE);

                if(documents.size() < 10) {
                    System.out.println(headlineStr);
                    StanfordCoreNLP pipeline = new StanfordCoreNLP(PropertiesUtils.asProperties(
                            "annotators", "tokenize,ssplit,pos,lemma",
                            "ssplit.isOneSentence", "true",
                            "tokenize.language", "en",
                            "tokenize.options", "americanize=true"));
                    Annotation annotation = new Annotation(headlineStr);
                    pipeline.annotate(annotation);
                    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
                    for (CoreMap sentence : sentences) {
                        for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                            String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                            lemma = lemma.toLowerCase();

                            if(!StopwordsUtils.getInstance().isStopWord(lemma)) {
                                String posStanford = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                                System.out.println("Lemma: " + lemma + " pos: " + posStanford);
                                try {
                                    POS pos = PosUtils.getWordnetPosMapping(posStanford);
                                    Set<String> synonyms = getSynonyms(pos, lemma);
                                    Set<String> antonyms = getAntonyms(pos, lemma);
                                    System.out.println("Synonyms:");
                                    for (String synonym : synonyms) {
                                        System.out.println("\t" + synonym);
                                    }
                                    System.out.println("Antonyms:");
                                    for (String antonym : antonyms) {
                                        System.out.println("\t" + antonym);
                                    }
                                } catch (JWNLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                Document document = new Document(
                        new Headline(headlineStr),
                        bodyId,
                        new Stance(Stance.getStanceID(stanceStr)));
                documents.add(document);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<String> getSynonyms(POS pos, String lemma) throws JWNLException {
        Set<String> synonyms = new HashSet<String>();
        if(pos != null) {
            IndexWord indexWord = dictionary.lookupIndexWord(pos, lemma);
            if (indexWord != null) {
                for (Synset synset : indexWord.getSenses()) {
                    for (Word word : synset.getWords()) {
                        synonyms.add(word.getLemma());
                    }
                }
            }
        } else {
            IndexWordSet indexWordSet = dictionary.lookupAllIndexWords(lemma);
            for (IndexWord indexWord : indexWordSet.getIndexWordCollection()) {
                if (indexWord != null) {
                    for (Synset synset : indexWord.getSenses()) {
                        for (Word word : synset.getWords()) {
                            synonyms.add(word.getLemma());
                        }
                    }
                }
            }
        }
        return synonyms;
    }

    private Set<String> getAntonyms(POS pos, String lemma) throws JWNLException {
        Set<String> antonyms = new HashSet<String>();
        if(pos != null) {
            IndexWord indexWord = dictionary.lookupIndexWord(pos, lemma);
            if (indexWord != null) {
                for (Synset synset : indexWord.getSenses()) {
                    PointerTargetNodeList pointerTargetNodes = PointerUtils.getAntonyms(synset);
                    for (PointerTargetNode pointerTargetNode : pointerTargetNodes) {
                        PointerTarget pointerTarget = pointerTargetNode.getPointerTarget();
                        Synset antSynset = pointerTarget.getSynset();
                        for (Word word : antSynset.getWords()) {
                            antonyms.add(word.getLemma());
                        }
                    }
                }
            }
        } else {
            IndexWordSet indexWordSet = dictionary.lookupAllIndexWords(lemma);
            for (IndexWord indexWord : indexWordSet.getIndexWordCollection()) {
                if (indexWord != null) {
                    for (Synset synset : indexWord.getSenses()) {
                        PointerTargetNodeList pointerTargetNodes = PointerUtils.getAntonyms(synset);
                        for (PointerTargetNode pointerTargetNode : pointerTargetNodes) {
                            PointerTarget pointerTarget = pointerTargetNode.getPointerTarget();
                            Synset antSynset = pointerTarget.getSynset();
                            for (Word word : antSynset.getWords()) {
                                antonyms.add(word.getLemma());
                            }
                        }
                    }
                }
            }
        }
        return antonyms;
    }

}
