package edu.arizona.cs.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by savan on 4/29/17.
 */
public class WordsUtils {
    private static final String STOP_WORDS = "/stopwords.txt";
    private static final String REFUTING_WORDS = "/refutingwords.txt";
    private static final String HEDGE_WORDS = "/hedgewords.txt";
    private static final String SUPPORTIVE_WORDS = "/supportivewords.txt";

    private static WordsUtils ourInstance = new WordsUtils();

    public static WordsUtils getInstance() {
        return ourInstance;
    }

    private Set<String> stopwordsSet;
    private Set<String> refutingSet;
    private Set<String> hedgeSet;
    private Set<String> supportiveSet;

    private WordsUtils() {
        stopwordsSet = new HashSet<String>();
        refutingSet = new HashSet<String>();
        hedgeSet = new HashSet<String>();
        supportiveSet = new HashSet<String>();

        try {
            BufferedReader stopReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(STOP_WORDS)));
            String line;
            while ((line = stopReader.readLine()) != null) {
                stopwordsSet.add(line);
            }
            stopReader.close();

            BufferedReader refuteReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(REFUTING_WORDS)));
            while ((line = refuteReader.readLine()) != null) {
                refutingSet.add(line);
            }

            refuteReader.close();

            BufferedReader hedgeReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(HEDGE_WORDS)));
            while ((line = hedgeReader.readLine()) != null) {
                hedgeSet.add(line);
            }

            hedgeReader.close();

            BufferedReader supportiveReader = new BufferedReader(
                    new InputStreamReader(WordsUtils.class.getResourceAsStream(SUPPORTIVE_WORDS)));
            while ((line = supportiveReader.readLine()) != null) {
                supportiveSet.add(line);
            }

            supportiveReader.close();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    public boolean isStopWord(String lemma) {
        return stopwordsSet.contains(lemma);
    }

    public boolean isRefutingWord(String lemma) {
        return refutingSet.contains(lemma);
    }

    public boolean isHedgeWord(String lemma) {
        return hedgeSet.contains(lemma);
    }

    public boolean isSuportiveWord(String lemma) {
        return supportiveSet.contains(lemma);
    }
}
