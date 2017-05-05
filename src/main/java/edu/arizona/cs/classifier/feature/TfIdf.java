package edu.arizona.cs.classifier.feature;

import edu.arizona.cs.data.Body;
import edu.arizona.cs.data.Headline;
import edu.arizona.cs.utils.LemmaCleanser;
import edu.arizona.cs.utils.WordsUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import net.sf.extjwnl.dictionary.Dictionary;

import java.util.*;

/**
 * Created by savan on 5/4/17.
 */
public class TfIdf implements Feature {
    private static final String NAME = "Tf_Idf";
    private static final long NUM_DOCS = 9615720;
    double score = 0;

    private Headline headline;
    private Body body;

    public TfIdf(Headline headline, Body body) {
        this.headline = headline;
        this.body = body;
    }

    public String getName() {
        return NAME;
    }

    public void setDictionary(Dictionary dictionary) {
        // Do nothing
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
        List<CoreLabel> headlineCoreLabels = headlineAnnot.get(CoreAnnotations.TokensAnnotation.class);
        Map<String, Integer> bodyTokens = new HashMap<String, Integer>();
        int totalTermCount = 0;

        for (CoreLabel coreLabel : bodyCoreLabels) {
            String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
            lemma = LemmaCleanser.getInstance().cleanse(lemma);
            //noinspection Since15
            if(!lemma.isEmpty() && !WordsUtils.getInstance().isStopWord(lemma)) {
                int count = 1;
                if(bodyTokens.containsKey(lemma)) {
                    count = bodyTokens.get(lemma);
                    count++;
                }
                bodyTokens.put(lemma, count);
                totalTermCount++;
            }
        }

        double tfIdfScore = 0;
        for (CoreLabel coreLabel : headlineCoreLabels) {
            String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
            lemma = LemmaCleanser.getInstance().cleanse(lemma);
            //noinspection Since15
            if(!lemma.isEmpty() && !WordsUtils.getInstance().isStopWord(lemma)) {
                double tfScore;
                double termCount = bodyTokens.get(lemma) != null ? bodyTokens.get(lemma).doubleValue() : 0;
                double tf = termCount / (double)totalTermCount;
                if(tf != 0) {
                    tfScore = Math.log(tf) + 1;
                } else {
                    tfScore = 0;
                }
                double idf = WordsUtils.getInstance().getIdfScores(lemma);
                double idfScore = idf == 0 ? 0 : Math.log(NUM_DOCS / idf);
                tfIdfScore += tfScore * idfScore;
            }
        }

        score = tfIdfScore;
    }

    public double getScore() {
        return score;
    }
}
