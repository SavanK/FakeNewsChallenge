package edu.arizona.cs.classifier.secondclassifier;

import edu.arizona.cs.data.Body;
import edu.arizona.cs.data.Headline;
import edu.arizona.cs.utils.LemmaCleanser;
import edu.arizona.cs.utils.WordsUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.PropertiesUtils;
import net.sf.extjwnl.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by savan on 5/5/17.
 */
public class Disagree extends AbstractStanceFeature {
    private static final String NAME = "Disagree";

    double score = 0;
    Dictionary dictionary;
    int n;

    private Headline headline;
    private Body body;

    public Disagree(Headline headline, Body body, int n) {
        super();
        this.headline = headline;
        this.body = body;
        this.n = n;
    }

    public String getName() {
        return NAME;
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public void computeScore() {
        //System.out.println("\t\t" + NAME + "-" + n + ", computing score ...");
        constructNGrams(headline, body, n);

        int disagreeCount = 0;

        for (Map.Entry<List<String>, Integer> headlineNGram : headlineNGrams.entrySet()) {
            String lastWord = headlineNGram.getKey().get(headlineNGram.getKey().size()-1);
            List<Pair<List<String>, Integer>> bodyNGramList = bodyNGrams.get(lastWord);
            if(bodyNGramList != null && bodyNGramList.size() > 0) {
                for (Pair<List<String>, Integer> bodyNGram : bodyNGramList){
                    if(headlineNGram.getValue() != SPIN_NEUTRAL &&
                            bodyNGram.second() == headlineNGram.getValue()) {
                        // if same spin
                        disagreeCount++;
                    }
                }
            }
        }

        score = disagreeCount; /// (double) headlineTokens.size();
    }

    public double getScore() {
        return score;
    }
}
