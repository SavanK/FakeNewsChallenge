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
 * Created by savan on 5/3/17.
 */
public class RefutingWords implements Feature {
    private static final String NAME = "Refuting_Words";
    double score = 0;
    int n;

    private Headline headline;
    private Body body;

    public RefutingWords(Headline headline, Body body) {
        this.headline = headline;
        this.body = body;
        n=3;
    }

    public String getName() {
        return NAME;
    }

    public void setDictionary(Dictionary dictionary) {
        // do nothing
    }

    public void computeScore() {
        //System.out.println("\t\t" + NAME + "-" + n + ", computing score ...");

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
            if(!lemma.isEmpty() && !WordsUtils.getInstance().isStopWord(lemma))
                bodyTokens.add(lemma);
        }

        for (CoreLabel coreLabel : headlineCoreLabels) {
            String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
            lemma = LemmaCleanser.getInstance().cleanse(lemma);
            //noinspection Since15
            if(!lemma.isEmpty() && !WordsUtils.getInstance().isStopWord(lemma)) {
                headlineTokens.add(lemma);
            }
        }

        Map<String, List<List<String>>> bodyNGrams = new HashMap<String, List<List<String>>>();

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
                List<List<String>> nGramList = bodyNGrams.get(lastWord);
                nGramList.add(nGram);
            } else {
                List<List<String>> nGramList = new ArrayList<List<String>>();
                nGramList.add(nGram);
                bodyNGrams.put(lastWord, nGramList);
            }
        }

        int refutingCount = 0;
        for (String word : headlineTokens) {
            if(bodyNGrams.containsKey(word)) {
                List<List<String>> nGramList = bodyNGrams.get(word);
                for (List<String> nGram : nGramList){
                    int i=0;
                    for (String prefix : nGram) {
                        if(i<nGram.size()-1) {
                            if(WordsUtils.getInstance().isRefutingWord(prefix)) {
                                refutingCount++;
                                break;
                            }
                        }
                        i++;
                    }
                }
            }
        }

        for (String word : bodyTokens) {
            if(WordsUtils.getInstance().isRefutingWord(word))
                refutingCount++;
        }

        score = refutingCount; /// (double) headlineTokens.size();
    }

    public double getScore() {
        return score;
    }
}
