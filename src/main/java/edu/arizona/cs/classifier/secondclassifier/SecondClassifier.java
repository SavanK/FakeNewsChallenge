package edu.arizona.cs.classifier.secondclassifier;

import de.bwaldvogel.liblinear.*;
import edu.arizona.cs.classifier.ClassLabel;
import edu.arizona.cs.classifier.feature.*;
import edu.arizona.cs.classifier.feature.Feature;
import edu.arizona.cs.data.DataRepo;
import edu.arizona.cs.data.Document;
import edu.arizona.cs.data.Headline;
import edu.arizona.cs.data.Stance;
import edu.arizona.cs.utils.ThreadPoolExecutorWrapper;
import net.sf.extjwnl.dictionary.Dictionary;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

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
public class SecondClassifier {
    private static final String MODEL = "src/main/resources/model_2";
    private static final String RESULT = "result/result_2.txt";
    private static final String CORRECT_DETECTION = "result/correct_detections_2.txt";
    private static final String INCORRECT_DETECTION = "result/incorrect_detections_2.txt";

    private Dictionary dictionary;
    private DataRepo dataRepo;
    private List<ClassLabel> classLabels;
    private ThreadPoolExecutor threadPoolExecutor;
    private Model model;
    private Map<Document, Stance> testDocuments;
    private List<Document> documents;

    public SecondClassifier(Dictionary dictionary, DataRepo dataRepo) {
        this.dictionary = dictionary;
        testDocuments = new HashMap<Document, Stance>();
        this.dataRepo = dataRepo;

        classLabels = new ArrayList<ClassLabel>();
        ClassLabel agree = new ClassLabel(new Stance(Stance.STANCE_AGREE));
        ClassLabel disagree = new ClassLabel(new Stance(Stance.STANCE_DISAGREE));
        ClassLabel discuss = new ClassLabel(new Stance(Stance.STANCE_DISCUSS));
        classLabels.add(agree);
        classLabels.add(disagree);
        classLabels.add(discuss);

        threadPoolExecutor = ThreadPoolExecutorWrapper.getInstance().getThreadPoolExecutor();
    }

    public void setTrainDocs(List<Document> documents) {
        this.documents = documents;
    }

    public void train() {
        System.out.println("Second classifier - Training in-progress...");

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
        problem.n = 3 + (int)problem.bias;
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
                Feature refutingWords = new RefutingWords(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
                document.addFeature(refutingWords);
                Feature hedgeFeature = new HedgeWords(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
                document.addFeature(hedgeFeature);
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
