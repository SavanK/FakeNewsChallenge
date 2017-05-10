package edu.arizona.cs.classifier.feature;

import edu.arizona.cs.classifier.feature.Feature;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by savan on 5/5/17.
 */
public abstract class AbstractStanceFeature implements Feature {
    protected static final int SPIN_POSITIVE = 1;
    protected static final int SPIN_NEGATIVE = -1;
    protected static final int SPIN_NEUTRAL = 0;
    protected Map<List<String>, Integer> headlineNGrams;
    protected Map<String, List<Pair<List<String>, Integer>>> bodyNGrams;

    public AbstractStanceFeature() {
        bodyNGrams = new HashMap<String, List<Pair<List<String>, Integer>>>();
        headlineNGrams = new HashMap<List<String>, Integer>();
    }

    /**
     * Construct nGrams and also find their spins (POSITIVE / NEGATIVE / NEUTRAL)
     * @param headline
     * @param body
     * @param n
     */
    protected void constructNGrams(Headline headline, Body body, int n) {
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
                if (!lemma.isEmpty() && !WordsUtils.getInstance().isContextPreservingStopWord(lemma))
                    bodyTokens.add(lemma);
            }

            for (CoreLabel coreLabel : headlineCoreLabels) {
                String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
                lemma = LemmaCleanser.getInstance().cleanse(lemma);
                //noinspection Since15
                if (!lemma.isEmpty() && !WordsUtils.getInstance().isContextPreservingStopWord(lemma)) {
                    headlineTokens.add(lemma);
                }
            }
        } else {
            bodyTokens = WordsUtils.getInstance().tokenize(body.getText());
            headlineTokens = WordsUtils.getInstance().tokenize(headline.getText());
        }

        if(bodyTokens.size() > n) {
            for (int i = 0; i < bodyTokens.size() - n; i++) {
                List<String> nGram = new ArrayList<String>();
                for (int j = 0; j < n; j++) {
                    nGram.add(bodyTokens.get(i + j));
                }

                String lastWord = nGram.get(nGram.size() - 1);
                if (bodyNGrams.containsKey(lastWord)) {
                    List<Pair<List<String>, Integer>> nGramList = bodyNGrams.get(lastWord);
                    nGramList.add(new Pair<List<String>, Integer>(nGram, getNgramSpin(nGram)));
                } else {
                    List<Pair<List<String>, Integer>> nGramList = new ArrayList<Pair<List<String>, Integer>>();
                    nGramList.add(new Pair<List<String>, Integer>(nGram, getNgramSpin(nGram)));
                    bodyNGrams.put(lastWord, nGramList);
                }
            }
        }

        if(headlineTokens.size() > n) {
            for (int i = 0; i < headlineTokens.size() - n; i++) {
                List<String> nGram = new ArrayList<String>();
                for (int j = 0; j < n; j++) {
                    nGram.add(headlineTokens.get(i + j));
                }
                headlineNGrams.put(nGram, getNgramSpin(nGram));
            }
        }
    }

    /**
     * In the nGram prefix, count the positive spins and negative spins.
     * Overall spin is determined by these values.
     * @param nGram
     * @return overallSpin of nGram
     */
    protected int getNgramSpin(List<String> nGram) {
        int spin = SPIN_NEUTRAL;

        int positiveSpinCount = 0;
        int negativeSpinCount = 0;

        for (String gram : nGram) {
            if(WordsUtils.getInstance().isSuportiveWord(gram)) {
                positiveSpinCount++;
            } else if(WordsUtils.getInstance().isRefutingWord(gram)) {
                negativeSpinCount++;
            }
        }

        if((positiveSpinCount == 0 && negativeSpinCount == 0) || (positiveSpinCount == negativeSpinCount)) {
            spin = SPIN_NEUTRAL;
        } else if(positiveSpinCount > negativeSpinCount) {
            spin = SPIN_POSITIVE;
        } else if(negativeSpinCount > positiveSpinCount) {
            spin = SPIN_NEGATIVE;
        }

        return spin;
    }
}
