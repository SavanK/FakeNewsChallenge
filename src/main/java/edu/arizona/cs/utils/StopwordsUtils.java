package edu.arizona.cs.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by savan on 4/29/17.
 */
public class StopwordsUtils {
    private static final String STOP_WORDS = "/stopwords.txt";

    private static StopwordsUtils ourInstance = new StopwordsUtils();

    public static StopwordsUtils getInstance() {
        return ourInstance;
    }

    private Set<String> stopwordsSet;

    private StopwordsUtils() {
        stopwordsSet = new HashSet<String>();

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(StopwordsUtils.class.getResourceAsStream(STOP_WORDS)));
            String line;
            while ((line = reader.readLine()) != null) {
                stopwordsSet.add(line);
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    public boolean isStopWord(String lemma) {
        return stopwordsSet.contains(lemma);
    }
}
