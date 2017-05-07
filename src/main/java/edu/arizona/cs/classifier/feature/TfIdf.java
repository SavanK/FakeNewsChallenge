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
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Created by savan on 5/4/17.
 */
public class TfIdf implements Feature {
    private static final String NAME = "Tf_Idf";
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

        List<String> headlineTokens = WordsUtils.getInstance().tokenize(headline.getText());
        List<String> bodyTokens = WordsUtils.getInstance().tokenize(body.getText());

        Map<String, Integer> bodyTokensFreq = new HashMap<String, Integer>();

        for(String token : bodyTokens) {
            int count;
            if(bodyTokensFreq.containsKey(token)) {
                count = bodyTokensFreq.get(token);
                count++;
            } else {
                count = 1;
            }
            bodyTokensFreq.put(token, count);
        }

        double tfIdfScore = 0;
        for(String token : headlineTokens) {
            double tfScore;
            int termCount = bodyTokensFreq.get(token) != null ? bodyTokensFreq.get(token) : 0;
            double tf = termCount / (double)bodyTokens.size();
            if(tf != 0) {
                tfScore = Math.log(tf) + 1;
            } else {
                tfScore = 0;
            }

            double idfScore = WordsUtils.getInstance().getIdfScores(token);

            tfIdfScore += (tfScore * idfScore);
        }

        score = tfIdfScore;
    }

    public double getScore() {
        return score;
    }
}
