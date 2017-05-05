package edu.arizona.cs.classifier.feature;

import edu.arizona.cs.data.Body;
import edu.arizona.cs.data.Headline;
import edu.arizona.cs.utils.LemmaCleanser;
import edu.arizona.cs.utils.PosUtils;
import edu.arizona.cs.utils.WordsUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.dictionary.Dictionary;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by savan on 5/2/17.
 */
public class Synonym implements Feature {
    private static final String NAME = "Synonym";
    double score = 0;
    Dictionary dictionary;

    private Headline headline;
    private Body body;

    public Synonym(Headline headline, Body body) {
        this.headline = headline;
        this.body = body;
    }

    public String getName() {
        return NAME;
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public void computeScore() {
        //System.out.println("\t\t" + NAME + ", computing score ...");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(PropertiesUtils.asProperties(
                "annotators", "tokenize,ssplit,pos,lemma",
                "ssplit.isOneSentence", "false",
                "tokenize.language", "en",
                "tokenize.options", "americanize=true"));

        Annotation bodyAnnot = new Annotation(body.getText());
        pipeline.annotate(bodyAnnot);
        Annotation headlineAnnot = new Annotation(headline.getText());
        pipeline.annotate(headlineAnnot);

        List<CoreLabel> bodyCoreLabels = bodyAnnot.get(CoreAnnotations.TokensAnnotation.class);
        Set<String> bodyTokens = new HashSet<String>();
        Set<String> headlineSynset = new HashSet<String>();

        for (CoreLabel coreLabel : bodyCoreLabels) {
            String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
            lemma = LemmaCleanser.getInstance().cleanse(lemma);
            //noinspection Since15
            if(!lemma.isEmpty() && !WordsUtils.getInstance().isStopWord(lemma))
                bodyTokens.add(lemma);
        }

        List<CoreMap> sentences = headlineAnnot.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
            for (CoreLabel coreLabel : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String pos = coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
                lemma = LemmaCleanser.getInstance().cleanse(lemma);
                //noinspection Since15
                if(!lemma.isEmpty() && !WordsUtils.getInstance().isStopWord(lemma)) {
                    try {
                        headlineSynset.add(lemma);
                        addSynonymsToSet(headlineSynset, PosUtils.getWordnetPosMapping(pos), lemma);
                    } catch (JWNLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        int coOccurenceCount = 0;
        for (String token : bodyTokens) {
            if(headlineSynset.contains(token))
                coOccurenceCount++;
        }

        score = coOccurenceCount / (double) (headlineSynset.size());//+bodyTokens.size());
    }

    public double getScore() {
        return score;
    }

    private void addSynonymsToSet(Set<String> synonymSet, POS pos, String lemma) throws JWNLException {
        if(pos != null) {
            IndexWord indexWord = dictionary.lookupIndexWord(pos, lemma);
            if (indexWord != null) {
                for (Synset synset : indexWord.getSenses()) {
                    for (Word word : synset.getWords()) {
                        synonymSet.add(word.getLemma());
                    }
                }
            }
        } else {
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
        }
    }

}
