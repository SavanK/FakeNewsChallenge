package edu.arizona.cs.classifier.feature;

import edu.arizona.cs.data.Body;
import edu.arizona.cs.data.Headline;
import edu.stanford.nlp.util.Pair;
import net.sf.extjwnl.dictionary.Dictionary;

import java.util.List;
import java.util.Map;

/**
 * Created by savan on 5/5/17.
 */
public class Discuss extends AbstractStanceFeature {
    private static final String NAME = "Discuss";

    double score = 0;
    Dictionary dictionary;
    int n;

    private Headline headline;
    private Body body;

    public Discuss(Headline headline, Body body, int n) {
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

        int discussCount = 0;

        for (Map.Entry<List<String>, Integer> headlineNGram : headlineNGrams.entrySet()) {
            String lastWord = headlineNGram.getKey().get(headlineNGram.getKey().size()-1);
            List<Pair<List<String>, Integer>> bodyNGramList = bodyNGrams.get(lastWord);
            if(bodyNGramList != null && bodyNGramList.size() > 0) {
                for (Pair<List<String>, Integer> bodyNGram : bodyNGramList){
                    if(headlineNGram.getValue() == SPIN_NEUTRAL &&
                            bodyNGram.second() == SPIN_NEUTRAL) {
                        // if both neutral spin
                        discussCount++;
                    }
                }
            }
        }

        score = discussCount / (double) bodyNGrams.size(); /// (double) headlineTokens.size();
    }

    public double getScore() {
        return score;
    }
}
