package edu.arizona.cs.classifier;

import de.bwaldvogel.liblinear.FeatureNode;
import edu.arizona.cs.data.Stance;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by savan on 4/29/17.
 */
public class ClassLabel {
    private Stance stance;

    public ClassLabel(Stance stance) {
        this.stance = stance;
    }

    public Stance getStance() {
        return stance;
    }
}
