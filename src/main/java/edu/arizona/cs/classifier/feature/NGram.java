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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by savan on 4/30/17.
 */
public class NGram implements Feature {
    private static final String NAME = "NGram";
    double score = 0;

    private Headline headline;
    private Body body;

    private int n;

    public NGram(Headline headline, Body body, int n) {
        this.headline = headline;
        this.body = body;
        this.n = n;
    }

    public String getName() {
        return NAME;
    }

    public void setDictionary(Dictionary dictionary) {
        // do nothing
    }

    public void computeScore() {
        //System.out.println("\t\t" + NAME + "-" + n + ", computing score ...");
        List<String> bodyTokens;
        List<String> headlineTokens;

        if(WordsUtils.LEMMATIZATION) {
            bodyTokens = new ArrayList<String>();
            headlineTokens = new ArrayList<String>();

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
                if (!lemma.isEmpty() && !WordsUtils.getInstance().isStopWord(lemma))
                    bodyTokens.add(lemma);
            }

            for (CoreLabel coreLabel : headlineCoreLabels) {
                String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
                lemma = LemmaCleanser.getInstance().cleanse(lemma);
                //noinspection Since15
                if (!lemma.isEmpty() && !WordsUtils.getInstance().isStopWord(lemma)) {
                    headlineTokens.add(lemma);
                }
            }
        } else {
            bodyTokens = WordsUtils.getInstance().tokenize(body.getText());
            headlineTokens = WordsUtils.getInstance().tokenize(headline.getText());
        }

        Set<String> headlineNGrams = new HashSet<String>();
        Set<String> bodyNGrams = new HashSet<String>();

        for(int i=0; i<=headlineTokens.size()-n;i++) {
            StringBuilder nGram = new StringBuilder();
            for(int j=0;j<n;j++) {
                if(i+j < headlineTokens.size()) {
                    nGram.append(headlineTokens.get(i+j));
                    if(j<n-1) {
                        nGram.append(",");
                    }
                } else {
                    break;
                }
            }
            headlineNGrams.add(nGram.toString());
        }

        if(headlineNGrams.size() > 0) {
            for (int i = 0; i <= bodyTokens.size() - n; i++) {
                StringBuilder nGram = new StringBuilder();
                for (int j = 0; j < n; j++) {
                    if (i + j < bodyTokens.size()) {
                        nGram.append(bodyTokens.get(i + j));
                        if (j < n - 1) {
                            nGram.append(",");
                        }
                    } else {
                        break;
                    }
                }
                bodyNGrams.add(nGram.toString());
            }

            int coOccurenceCount = 0;
            for (String nGram : headlineNGrams) {
                if (bodyNGrams.contains(nGram)) {
                    coOccurenceCount++;
                }
            }

            score = coOccurenceCount / (double) (headlineNGrams.size());//+bodyTokens.size());
        } else {
            score = 0;
        }
    }

    public double getScore() {
        return score;
    }
}
