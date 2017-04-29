package edu.arizona.cs.utils;

import net.sf.extjwnl.data.POS;

/**
 * Created by savan on 4/29/17.
 */
public class PosUtils {
    /**
     * Stanford Core NLP POS abbreviations
     * */
    // Adjective
    private static final String JJ = "JJ";
    private static final String JJR = "JJR";
    private static final String JJS = "JJS";

    // Noun
    private static final String NN = "NN";
    private static final String NNS = "NNS";
    private static final String NNP = "NNP";
    private static final String NNPS = "NNPS";

    // Adverb
    private static final String RB = "RB";
    private static final String RBR = "RBR";
    private static final String RBS = "RBS";

    // Verb
    private static final String VB = "VB";
    private static final String VBD = "VBD";
    private static final String VBG = "VBG";
    private static final String VBN = "VBN";
    private static final String VBP = "VBP";
    private static final String VBZ = "VBZ";

    public static POS getWordnetPosMapping(String stanfordPos) {
        if(stanfordPos.compareToIgnoreCase(JJ) == 0 ||
                stanfordPos.compareToIgnoreCase(JJR) == 0 ||
                stanfordPos.compareToIgnoreCase(JJS) == 0) {
            return POS.ADJECTIVE;
        } else if(stanfordPos.compareToIgnoreCase(NN) == 0 ||
                stanfordPos.compareToIgnoreCase(NNS) == 0 ||
                stanfordPos.compareToIgnoreCase(NNP) == 0 ||
                stanfordPos.compareToIgnoreCase(NNPS) == 0) {
            return POS.NOUN;
        } else if(stanfordPos.compareToIgnoreCase(RB) == 0 ||
                stanfordPos.compareToIgnoreCase(RBR) == 0 ||
                stanfordPos.compareToIgnoreCase(RBS) == 0) {
            return POS.ADVERB;
        } else if(stanfordPos.compareToIgnoreCase(VB) == 0 ||
                stanfordPos.compareToIgnoreCase(VBD) == 0 ||
                stanfordPos.compareToIgnoreCase(VBG) == 0 ||
                stanfordPos.compareToIgnoreCase(VBN) == 0 ||
                stanfordPos.compareToIgnoreCase(VBP) == 0 ||
                stanfordPos.compareToIgnoreCase(VBZ) == 0) {
            return POS.VERB;
        } else {
            // default to NOUN
            return null;
        }
    }

}
