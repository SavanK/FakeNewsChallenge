package edu.arizona.cs.classifier.thirdclassifier;

import de.bwaldvogel.liblinear.*;
import edu.arizona.cs.classifier.ClassLabel;
import edu.arizona.cs.classifier.feature.*;
import edu.arizona.cs.classifier.feature.Feature;
import edu.arizona.cs.classifier.secondclassifier.Agree;
import edu.arizona.cs.classifier.secondclassifier.Disagree;
import edu.arizona.cs.classifier.secondclassifier.Discuss;
import edu.arizona.cs.data.DataRepo;
import edu.arizona.cs.data.Document;
import edu.arizona.cs.data.Stance;
import edu.arizona.cs.utils.ThreadPoolExecutorWrapper;
import net.sf.extjwnl.dictionary.Dictionary;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by savan on 4/29/17.
 */
public class ThirdClassifier {
    private static final String MODEL = "src/main/resources/model_3";
    private static final String RESULT = "result/result_3.txt";
    private static final String CORRECT_DETECTION = "result/correct_detections_3.txt";
    private static final String INCORRECT_DETECTION = "result/incorrect_detections_3.txt";

    private Dictionary dictionary;
    private DataRepo dataRepo;
    private List<ClassLabel> classLabels;
    private ThreadPoolExecutor threadPoolExecutor;
    private Model model;
    private Map<Document, Stance> testDocuments;
    private List<Document> documents;

    public ThirdClassifier(Dictionary dictionary, DataRepo dataRepo) {
        this.dictionary = dictionary;
        testDocuments = new HashMap<Document, Stance>();
        this.dataRepo = dataRepo;

        classLabels = new ArrayList<ClassLabel>();
        ClassLabel disagree = new ClassLabel(new Stance(Stance.STANCE_DISAGREE));
        ClassLabel agree = new ClassLabel(new Stance(Stance.STANCE_AGREE));
        classLabels.add(agree);
        classLabels.add(disagree);

        threadPoolExecutor = ThreadPoolExecutorWrapper.getInstance().getThreadPoolExecutor();
    }

    public void setTrainDocs(List<Document> documents) {
        this.documents = documents;
    }

    public void train() {
        System.out.println("Third classifier - Training in-progress...");

        double y[] = new double[documents.size()];
        FeatureNode x[][] = new FeatureNode[documents.size()][];

        List<FeatureExtractionTask> featureExtractionTasks = new ArrayList<FeatureExtractionTask>();
        for (Document document : documents) {
            FeatureExtractionCallable featureExtractionCallable = new FeatureExtractionCallable(document);
            FeatureExtractionTask featureExtractionTask =
                    new FeatureExtractionTask(featureExtractionCallable);
            featureExtractionTasks.add(featureExtractionTask);
            threadPoolExecutor.execute(featureExtractionTask);
        }

        waitUntilTasksComplete(featureExtractionTasks);

        int i=0;
        for (Document document : documents) {
            y[i] = getClassLabelIndex(document.getStance());
            x[i] = document.getSparseFeaturesScore();
            i++;
        }

        System.out.println("Creating second classifier model");

        Problem problem = new Problem();
        problem.bias = 1;
        problem.l = documents.size();
        problem.n = 4 + (int)problem.bias;
        problem.y = y;
        problem.x = x;

        SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.001; // stopping criteria

        Parameter parameter = new Parameter(solver, C, eps);
        model = Linear.train(problem, parameter);
        File modelFile = new File(MODEL);
        try {
            model.save(modelFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void classify(Map<Document, Stance> testDocs) throws FileNotFoundException {
        testDocuments = testDocs;

        List<FeatureExtractionTask> featureExtractionTasks = new ArrayList<FeatureExtractionTask>();
        for (Map.Entry<Document, Stance> documentStanceEntry : testDocuments.entrySet()) {
            FeatureExtractionCallable featureExtractionCallable =
                    new FeatureExtractionCallable(documentStanceEntry.getKey());
            FeatureExtractionTask featureExtractionTask =
                    new FeatureExtractionTask(featureExtractionCallable);
            featureExtractionTasks.add(featureExtractionTask);
            threadPoolExecutor.execute(featureExtractionTask);
        }

        waitUntilTasksComplete(featureExtractionTasks);

        Writer result = null;
        Writer correct = null;
        Writer incorrect = null;
        try {
            result = new FileWriter(RESULT);
            correct = new FileWriter(CORRECT_DETECTION);
            incorrect = new FileWriter(INCORRECT_DETECTION);
            correct.append("Headline,Body ID,Detected Stance,Actual Stance\n");
            incorrect.append("Headline,Body ID,Detected Stance,Actual Stance\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        int correctDetections = 0;
        int wrongDetections = 0;
        for (Map.Entry<Document, Stance> documentStanceEntry : testDocuments.entrySet()) {
            Document document = documentStanceEntry.getKey();
            Stance actualStance = documentStanceEntry.getValue();
            int index = (int)Linear.predict(model, document.getSparseFeaturesScore());
            document.setStance(classLabels.get(index).getStance());

            if(actualStance.getStance() == document.getStance().getStance()) {
                correctDetections++;
                if(correct != null) {
                    try {
                        correct.append(document.getHeadline().getText() + "," + document.getBodyId() + "," +
                                document.getStance().getStanceStr() + "," + actualStance.getStanceStr() + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                wrongDetections++;
                if(incorrect != null) {
                    try {
                        incorrect.append(document.getHeadline().getText() + "," + document.getBodyId() + "," +
                                document.getStance().getStanceStr() + "," + actualStance.getStanceStr() + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        System.out.println("Correct detections: " + correctDetections + " Wrong detections: " + wrongDetections);

        if(result != null) {
            try {
                result.append("Correct detections: " + correctDetections + " Wrong detections: " + wrongDetections + "\n");
                result.flush();
                result.close();
                correct.flush();
                correct.close();
                incorrect.flush();
                incorrect.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void waitUntilTasksComplete(List<FeatureExtractionTask> futureTasks) {
        boolean featureExtractionComplete = false;
        while(!featureExtractionComplete) {
            try {
                Thread.sleep(10000);
                int tasksCompleted = 0;
                for (FutureTask futureTask : futureTasks) {
                    if(futureTask.isDone()) {
                        tasksCompleted++;
                    }
                }
                System.out.println("Completed extracting features for docs: " + tasksCompleted + "/" + futureTasks.size());

                if(tasksCompleted == futureTasks.size()) {
                    // All tasks completed
                    featureExtractionComplete = true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int getClassLabelIndex(Stance stance) {
        if(stance.getStance() == Stance.STANCE_AGREE) {
            return 0;
        } else {
            return 1;
        }
    }

    private class FeatureExtractionCallable implements Callable<Document> {
        private Document document;

        public FeatureExtractionCallable(Document document) {
            this.document = document;
        }

        public Document getDocument() {
            return document;
        }

        public Document call() throws Exception {
            /*System.out.println("\tExtracting features from doc:" + docIndex +
                    " by thread:" + Thread.currentThread().getId());*/
            try {
                Feature agree = new Agree(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
                agree.setDictionary(dictionary);
                document.addFeature(agree);
                Feature disagree = new Disagree(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
                disagree.setDictionary(dictionary);
                document.addFeature(disagree);
                Feature refutingWords = new RefutingWords(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
                document.addFeature(refutingWords);
                Feature supportiveFeature = new SupportiveWords(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
                document.addFeature(supportiveFeature);
            } catch (Exception e) {
                System.out.println("Exception for doc: " + document.getHeadline() +
                        ", bodyID:" + document.getBodyId() + e.getCause());
            }
            return document;
        }
    }

    private class FeatureExtractionTask extends FutureTask<Document> {
        private Document document;

        public FeatureExtractionTask(Callable<Document> callable) {
            super(callable);
            document = ((FeatureExtractionCallable)callable).getDocument();
        }

        public Document getDocument() {
            return document;
        }
    }

}
