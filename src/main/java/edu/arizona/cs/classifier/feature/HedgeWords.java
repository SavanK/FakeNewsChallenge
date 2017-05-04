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

import java.util.List;

/**
 * Created by savan on 5/3/17.
 */
public class HedgeWords implements Feature {
    private static final String NAME = "Hedge_Words";
    double score = 0;

    private Headline headline;
    private Body body;

    public HedgeWords(Headline headline, Body body) {
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

        StanfordCoreNLP pipeline = new StanfordCoreNLP(PropertiesUtils.asProperties(
                "annotators", "tokenize,ssplit,pos,lemma",
                "ssplit.isOneSentence", "false",
                "tokenize.language", "en",
                "tokenize.options", "americanize=true"));

        Annotation bodyAnnot = new Annotation(body.getText());
        pipeline.annotate(bodyAnnot);

        List<CoreLabel> bodyCoreLabels = bodyAnnot.get(CoreAnnotations.TokensAnnotation.class);
        int bodyTokenCount=0;
        int hedgeWordCount=0;

        for (CoreLabel coreLabel : bodyCoreLabels) {
            String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
            lemma = LemmaCleanser.getInstance().cleanse(lemma);
            //noinspection Since15
            if(!lemma.isEmpty() && !WordsUtils.getInstance().isStopWord(lemma) &&
                    WordsUtils.getInstance().isHedgeWord(lemma)) {
                hedgeWordCount++;
            }
            bodyTokenCount++;
        }

        score = hedgeWordCount / (double) bodyTokenCount;
    }

    public double getScore() {
        return score;
    }
}
