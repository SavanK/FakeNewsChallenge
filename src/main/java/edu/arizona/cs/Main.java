package edu.arizona.cs;

import edu.arizona.cs.classifier.firstclassifier.FirstClassifier;
import edu.arizona.cs.data.DataRepo;
import edu.arizona.cs.data.Document;
import edu.arizona.cs.data.Stance;
import edu.arizona.cs.utils.Scorer;
import edu.arizona.cs.utils.ThreadPoolExecutorWrapper;
import edu.arizona.cs.utils.WordsUtils;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;

import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Created by savan on 4/28/17.
 */
public class Main {

    private static final String TRAIN_STANCES = "src/main/resources/fnc_2/train_stances_csc483583.csv";
    private static final String TRAIN_BODIES = "src/main/resources/fnc_2/train_bodies.csv";
    private static final String WN_PROPERTIES = "/wn_file_properties.xml";
    private static final String TEST_STANCES = "src/main/resources/fnc_2/test_stances_csc483583.csv";

    private static Dictionary dictionary;
    private static DataRepo dataRepo;
    private static FirstClassifier firstClassifier;

    public static void main(String[] args) {
        try {
            dictionary = Dictionary.getInstance(Main.class.getResourceAsStream(WN_PROPERTIES));
            WordsUtils.getInstance().setDictionary(dictionary);
            dataRepo = new DataRepo(dictionary);
            dataRepo.readData(TRAIN_STANCES, TRAIN_BODIES);

            firstClassifier = new FirstClassifier(dataRepo, dictionary);
            firstClassifier.train();
            Map<Document, Stance> result = firstClassifier.classify(TEST_STANCES);
            ThreadPoolExecutorWrapper.getInstance().getThreadPoolExecutor().shutdown();
            Scorer.getInstance().score(result);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JWNLException e) {
            e.printStackTrace();
        }
    }
}