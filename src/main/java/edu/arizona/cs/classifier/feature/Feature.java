package edu.arizona.cs.classifier.feature;

import edu.arizona.cs.data.Body;
import edu.arizona.cs.data.Headline;
import net.sf.extjwnl.dictionary.Dictionary;

/**
 * Created by savan on 4/29/17.
 */
public interface Feature {
    String getName();
    void setDictionary(Dictionary dictionary);
    void computeScore();
    double getScore();
}
