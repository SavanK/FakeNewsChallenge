package edu.arizona.cs.classifier.secondclassifier;

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

    protected void constructNGrams(Headline headline, Body body, int n) {
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
        List<String> bodyTokens = new ArrayList<String>();
        List<String> headlineTokens = new ArrayList<String>();

        for (CoreLabel coreLabel : bodyCoreLabels) {
            String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
            lemma = LemmaCleanser.getInstance().cleanse(lemma);
            //noinspection Since15
            if(!lemma.isEmpty() && !WordsUtils.getInstance().isContextPreservingStopWord(lemma))
                bodyTokens.add(lemma);
        }

        for (CoreLabel coreLabel : headlineCoreLabels) {
            String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
            lemma = LemmaCleanser.getInstance().cleanse(lemma);
            //noinspection Since15
            if(!lemma.isEmpty() && !WordsUtils.getInstance().isContextPreservingStopWord(lemma)) {
                headlineTokens.add(lemma);
            }
        }

        for (int i = 0; i <= bodyTokens.size() - n; i++) {
            List<String> nGram = new ArrayList<String>();
            for (int j = 0; j < n; j++) {
                if (i + j < bodyTokens.size()) {
                    nGram.add(bodyTokens.get(i + j));
                } else {
                    break;
                }
            }

            String lastWord = nGram.get(nGram.size()-1);
            if(bodyNGrams.containsKey(lastWord)) {
                List<Pair<List<String>, Integer>> nGramList = bodyNGrams.get(lastWord);
                nGramList.add(new Pair<List<String>, Integer>(nGram, getNgramSpin(nGram)));
            } else {
                List<Pair<List<String>, Integer>> nGramList = new ArrayList<Pair<List<String>, Integer>>();
                nGramList.add(new Pair<List<String>, Integer>(nGram, getNgramSpin(nGram)));
                bodyNGrams.put(lastWord, nGramList);
            }
        }

        for(int i=0; i<=headlineTokens.size()-n;i++) {
            List<String> nGram = new ArrayList<String>();
            for(int j=0;j<n;j++) {
                if(i+j < headlineTokens.size()) {
                    nGram.add(headlineTokens.get(i+j));
                } else {
                    break;
                }
            }
            headlineNGrams.put(nGram, getNgramSpin(nGram));
        }
    }

    protected int getNgramSpin(List<String> nGram) {
        int spin1 = SPIN_NEUTRAL;
        int spin2 = SPIN_NEUTRAL;

        if(nGram.size() > 1) {
            if(WordsUtils.getInstance().isRefutingWord(nGram.get(0))) {
                spin1 = SPIN_NEGATIVE;
            } else if(WordsUtils.getInstance().isSuportiveWord(nGram.get(0))) {
                spin1 = SPIN_POSITIVE;
            }
        }

        if(nGram.size() > 2) {
            if(WordsUtils.getInstance().isRefutingWord(nGram.get(1))) {
                spin1 = SPIN_NEGATIVE;
            } else if(WordsUtils.getInstance().isSuportiveWord(nGram.get(1))) {
                spin1 = SPIN_POSITIVE;
            }
        }

        int spin;

        /*if(spin1 == SPIN_NEUTRAL || spin1 == SPIN_POSITIVE) {
            spin = spin2;
        } else {
            // spin1 == SPIN_NEGATIVE
            if(spin2 == SPIN_NEGATIVE) {
                spin = SPIN_POSITIVE;
            } else if(spin2 == SPIN_POSITIVE) {
                spin = SPIN_NEGATIVE;
            } else {
                spin = SPIN_NEGATIVE;
            }
        }*/
        spin = spin2;

        return spin;
    }
}
