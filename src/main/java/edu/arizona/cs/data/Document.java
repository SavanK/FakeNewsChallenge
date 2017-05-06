package edu.arizona.cs.data;

import de.bwaldvogel.liblinear.FeatureNode;
import edu.arizona.cs.classifier.feature.Feature;
import javafx.util.Pair;

import java.util.*;

/**
 * Created by savan on 4/28/17.
 */
public class Document {
    private Headline headline;
    private int bodyId;
    private Stance stance;
    private List<Feature> features;

    public Document(Headline headline, int bodyId, Stance stance) {
        this.headline = headline;
        this.bodyId = bodyId;
        this.stance = stance;
        features = new ArrayList<Feature>();
    }

    public Headline getHeadline() {
        return headline;
    }

    public int getBodyId() {
        return bodyId;
    }

    public Stance getStance() {
        return stance;
    }

    public void setStance(Stance stance) {
        this.stance = stance;
    }

    public void addFeature(Feature feature) {
        feature.computeScore();
        features.add(feature);
    }

    public void clearFeatures() {
        features.clear();
    }

    public FeatureNode[] getSparseFeaturesScore() {
        List<Pair<Integer, Double>> sparseFeatureScores = new ArrayList<Pair<Integer, Double>>();
        int i=1;
        for(Feature feature : features) {
            if(feature.getScore() != 0) {
                sparseFeatureScores.add(new Pair<Integer, Double>(i, feature.getScore()));
            }
            i++;
        }
        // for bias
        sparseFeatureScores.add(new Pair<Integer, Double>(i, (double)1));

        Collections.sort(sparseFeatureScores, new Comparator<Pair<Integer, Double>>() {
            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                return o1.getKey() - o2.getKey();
            }
        });

        FeatureNode[] spareScores = new FeatureNode[sparseFeatureScores.size()];
        i=0;
        for (Pair<Integer, Double> pair : sparseFeatureScores) {
            spareScores[i] = new FeatureNode(pair.getKey(), pair.getValue());
            i++;
        }

        return spareScores;
    }
}
