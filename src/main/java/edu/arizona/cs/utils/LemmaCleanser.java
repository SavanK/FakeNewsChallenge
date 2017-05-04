package edu.arizona.cs.utils;

/**
 * Created by savan on 5/2/17.
 */
public class LemmaCleanser {
    private static LemmaCleanser ourInstance = new LemmaCleanser();

    public static LemmaCleanser getInstance() {
        return ourInstance;
    }

    private LemmaCleanser() {
    }

    public String cleanse(String lemma) {
        // to lowercase
        lemma.toLowerCase();

        // set lemma to null if http link
        if(lemma.startsWith("http://")) {
            lemma = "";
        }
        return lemma;
    }
}
