package edu.arizona.cs.classifier.firstclassifier;

import de.bwaldvogel.liblinear.*;
import edu.arizona.cs.classifier.ClassLabel;
import edu.arizona.cs.classifier.feature.*;
import edu.arizona.cs.classifier.feature.Feature;
import edu.arizona.cs.classifier.secondclassifier.SecondClassifier;
import edu.arizona.cs.data.*;
import edu.arizona.cs.utils.ThreadPoolExecutorWrapper;
import net.sf.extjwnl.dictionary.Dictionary;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by savan on 4/29/17.
 */
public class FirstClassifier {
    private static final String MODEL = "src/main/resources/model_1";
    private static final String RESULT = "result/result_1.txt";
    private static final String CORRECT_DETECTION = "result/correct_detections_1.txt";
    private static final String INCORRECT_DETECTION = "result/incorrect_detections_1.txt";

    private DataRepo dataRepo;
    private Dictionary dictionary;
    private List<ClassLabel> classLabels;
    private ThreadPoolExecutor threadPoolExecutor;
    private Model model;
    private Map<Document, Stance> testDocuments;
    private SecondClassifier secondClassifier;

    public FirstClassifier(DataRepo dataRepo, Dictionary dictionary) {
        this.dataRepo = dataRepo;
        this.dictionary = dictionary;
        testDocuments = new HashMap<Document, Stance>();
        secondClassifier = new SecondClassifier(dictionary, dataRepo);

        classLabels = new ArrayList<ClassLabel>();
        ClassLabel tempRelated = new ClassLabel(new Stance(Stance.STANCE_TEMP_RELATED));
        ClassLabel unrelated = new ClassLabel(new Stance(Stance.STANCE_UNRELATED));
        classLabels.add(unrelated);
        classLabels.add(tempRelated);

        threadPoolExecutor = ThreadPoolExecutorWrapper.getInstance().getThreadPoolExecutor();
    }

    public void train() {
        System.out.println("First classifier - Training in-progress...");
        List<Document> secondClassiferDocs = new ArrayList<Document>();

        double y[] = new double[dataRepo.getDocuments().size()];
        FeatureNode x[][] = new FeatureNode[dataRepo.getDocuments().size()][];

        List<FeatureExtractionTask> featureExtractionTasks = new ArrayList<FeatureExtractionTask>();
        for (Document document : dataRepo.getDocuments()) {
            FeatureExtractionCallable featureExtractionCallable = new FeatureExtractionCallable(document);
            FeatureExtractionTask featureExtractionTask =
                    new FeatureExtractionTask(featureExtractionCallable);
            featureExtractionTasks.add(featureExtractionTask);
            threadPoolExecutor.execute(featureExtractionTask);
            if(document.getStance().getStance() != Stance.STANCE_UNRELATED) {
                secondClassiferDocs.add(document);
            }
        }

        waitUntilTasksComplete(featureExtractionTasks);

        int i=0;
        for (Document document : dataRepo.getDocuments()) {
            y[i] = getClassLabelIndex(document.getStance());
            x[i] = document.getSparseFeaturesScore();
            i++;
        }

        System.out.println("Creating first classifier model");

        Problem problem = new Problem();
        problem.bias = 1;
        problem.l = dataRepo.getDocuments().size();
        problem.n = 5 + (int)problem.bias;
        problem.y = y;
        problem.x = x;

        SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria

        Parameter parameter = new Parameter(solver, C, eps);

        /*double target[] = new double[problem.l];
        Linear.crossValidation(problem, parameter, 100, target);

        int correctCount = 0;
        i=0;
        for (Document document : dataRepo.getDocuments()) {
            Stance detectedStance = classLabels.get((int)(target[i])).getStance();
            if((detectedStance.getStance() == Stance.STANCE_UNRELATED &&
                    document.getStance().getStance() == Stance.STANCE_UNRELATED) ||
                    (detectedStance.getStance() == Stance.STANCE_TEMP_RELATED &&
                            document.getStance().getStance() != Stance.STANCE_UNRELATED)) {
                correctCount++;
            }
            i++;
        }

        System.out.println("Accuracy: " + (double) (correctCount/dataRepo.getDocuments().size()));*/

        model = Linear.train(problem, parameter);
        File modelFile = new File(MODEL);
        try {
            model.save(modelFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Document document : secondClassiferDocs) {
            document.clearFeatures();
        }
        secondClassifier.setTrainDocs(secondClassiferDocs);
        secondClassifier.train();
    }

    public Map<Document, Stance> classify(String testStancesPath) throws FileNotFoundException {
        Reader in = new FileReader(testStancesPath);
        System.out.println("Extracting features in test docs in-progress...");
        try {
            CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader());
            for (CSVRecord record : parser) {
                int bodyId = Integer.parseInt(record.get(DataRepo.BODY_ID));
                String headlineStr = record.get(DataRepo.HEADLINE);
                String stanceStr = record.get(DataRepo.STANCE);
                Stance actualStance = new Stance(Stance.getStanceID(stanceStr));

                Document document = new Document(
                        new Headline(headlineStr),
                        bodyId,
                        new Stance(Stance.STANCE_UNCLASSIFIED));
                testDocuments.put(document, actualStance);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        Map<Document, Stance> documentStanceMapNextClassifier = new HashMap<Document, Stance>();
        for (Map.Entry<Document, Stance> documentStanceEntry : testDocuments.entrySet()) {
            Document document = documentStanceEntry.getKey();
            Stance actualStance = documentStanceEntry.getValue();
            int index = (int)Linear.predict(model, document.getSparseFeaturesScore());

            document.setStance(classLabels.get(index).getStance());

            if(actualStance.getStance() == document.getStance().getStance() ||
                    (actualStance.getStance() != Stance.STANCE_UNRELATED && document.getStance().getStance() == Stance.STANCE_TEMP_RELATED)) {
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

            if(document.getStance().getStance() == Stance.STANCE_TEMP_RELATED) {
                document.setStance(new Stance(Stance.STANCE_UNCLASSIFIED));
                document.clearFeatures();
                documentStanceMapNextClassifier.put(document, actualStance);
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

        secondClassifier.classify(documentStanceMapNextClassifier);

        return testDocuments;
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
        if(stance.getStance() == Stance.STANCE_UNRELATED) {
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
                Feature bagOfWords = new BagOfWords(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
                document.addFeature(bagOfWords);
                Feature tfIdf = new TfIdf(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
                document.addFeature(tfIdf);
                Feature binaryCoOccurrence = new BinaryCoOccurrence(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()));
                document.addFeature(binaryCoOccurrence);
                Feature nGram_2 = new NGram(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()), 2);
                document.addFeature(nGram_2);
                Feature nGram_3 = new NGram(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()), 3);
                document.addFeature(nGram_3);
                /*Feature nGram_4 = new NGram(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()), 4);
                document.addFeature(nGram_4);
                Feature nGram_5 = new NGram(document.getHeadline(), dataRepo.getBodies().get(document.getBodyId()), 5);
                document.addFeature(nGram_5);*/
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
