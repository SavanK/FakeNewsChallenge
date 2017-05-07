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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by savan on 4/29/17.
 */
public class BagOfWords implements Feature {

    private static final String NAME = "Bag_of_Words";

    private Headline headline;
    private Body body;
    double score = 0;

    public BagOfWords(Headline headline, Body body) {
        this.headline = headline;
        this.body = body;
    }

    public String getName() {
        return NAME;
    }

    public void setDictionary(Dictionary dictionary) {
        // do nothing
    }

    public void computeScore() {
        //System.out.println("\t\t" + NAME + ", computing score ...");
        Set<String> bodyTokens = new HashSet<String>();
        Set<String> headlineTokens = new HashSet<String>();

        if(WordsUtils.LEMMATIZATION) {
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

            for (CoreLabel coreLabel : bodyCoreLabels) {
                String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
                lemma = LemmaCleanser.getInstance().cleanse(lemma);
                //noinspection Since15
                if (!lemma.isEmpty())
                    bodyTokens.add(lemma);
            }

            for (CoreLabel coreLabel : headlineCoreLabels) {
                String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
                lemma = LemmaCleanser.getInstance().cleanse(lemma);
                //noinspection Since15
                if (!lemma.isEmpty())
                    headlineTokens.add(lemma);
            }
        } else {
            bodyTokens.addAll(WordsUtils.getInstance().tokenize(body.getText()));
            headlineTokens.addAll(WordsUtils.getInstance().tokenize(headline.getText()));
        }

        Set<String> union = new HashSet<String>();
        Set<String> intersection = new HashSet<String>();

        intersection.addAll(headlineTokens);
        intersection.retainAll(bodyTokens);

        union.addAll(headlineTokens);
        union.addAll(bodyTokens);

        score = intersection.size() / (double) union.size();
    }

    public double getScore() {
        return score;
    }
}
