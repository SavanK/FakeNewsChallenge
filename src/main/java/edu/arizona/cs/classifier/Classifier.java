package edu.arizona.cs.classifier;

import de.bwaldvogel.liblinear.*;
import edu.arizona.cs.classifier.feature.*;
import edu.arizona.cs.classifier.feature.Feature;
import edu.arizona.cs.data.*;
import net.sf.extjwnl.dictionary.Dictionary;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by savan on 4/29/17.
 */
public class Classifier {
    private DataRepo dataRepo;
    private Dictionary dictionary;
    private List<ClassLabel> classLabels;

    public Classifier(DataRepo dataRepo, Dictionary dictionary) {
        this.dataRepo = dataRepo;
        this.dictionary = dictionary;

        classLabels = new ArrayList<ClassLabel>();
        ClassLabel agree = new ClassLabel(new Stance(Stance.STANCE_AGREE));
        ClassLabel disagree = new ClassLabel(new Stance(Stance.STANCE_DISAGREE));
        ClassLabel discuss = new ClassLabel(new Stance(Stance.STANCE_DISCUSS));
        ClassLabel unrelated = new ClassLabel(new Stance(Stance.STANCE_UNRELATED));
        classLabels.add(agree);
        classLabels.add(disagree);
        classLabels.add(discuss);
        classLabels.add(unrelated);
    }

    public void train() {
        System.out.println("Training in-progress...");

        int i = 0;
        double y[] = new double[dataRepo.getDocuments().size()];
        FeatureNode x[][] = new FeatureNode[dataRepo.getDocuments().size()][];
        for (Document document : dataRepo.getDocuments()) {
            System.out.println("\tExtracting features from doc:" + i + "...");

            Feature bagOfWords = new BagOfWords(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
            document.addFeature(bagOfWords);
            Feature binaryCoOccurrence = new BinaryCoOccurrence(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
            document.addFeature(binaryCoOccurrence);
            Feature nGram_2 = new NGram(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()), 2);
            document.addFeature(nGram_2);
            Feature nGram_3 = new NGram(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()), 3);
            document.addFeature(nGram_3);
            Feature nGram_4 = new NGram(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()), 4);
            document.addFeature(nGram_4);
            Feature nGram_5 = new NGram(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()), 5);
            document.addFeature(nGram_5);
            Feature synonym = new Synonym(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
            synonym.setDictionary(dictionary);
            document.addFeature(synonym);
            Feature antonym = new Antonym(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
            antonym.setDictionary(dictionary);
            document.addFeature(antonym);

            y[i] = getClassLabelIndex(document.getStance());
            x[i] = document.getSparseFeaturesScore();

            i++;
        }

        System.out.println("Creating classifier");

        Problem problem = new Problem();
        problem.l = dataRepo.getDocuments().size();
        problem.n = 6;
        problem.y = y;
        problem.x = x;
        problem.bias = 0;

        SolverType solver = SolverType.L2R_LR; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria

        Parameter parameter = new Parameter(solver, C, eps);
        Model model = Linear.train(problem, parameter);
        File modelFile = new File("model");
        try {
            model.save(modelFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Stance classify(Headline headline, int bodyId) {
        Stance stance = null;

        return stance;
    }

    private ClassLabel getClassLabel(Stance stance) {
        ClassLabel searchedClassLabel = null;
        for (ClassLabel classLabel : classLabels) {
            if(classLabel.getStance().equals(stance)) {
                searchedClassLabel = classLabel;
                break;
            }
        }
        return searchedClassLabel;
    }

    private int getClassLabelIndex(Stance stance) {
        int index = 0;
        int searchedIndex = 0;
        for (ClassLabel classLabel : classLabels) {
            if(classLabel.getStance().equals(stance)) {
                searchedIndex = index;
                break;
            }
            index++;
        }
        return searchedIndex;
    }

}
