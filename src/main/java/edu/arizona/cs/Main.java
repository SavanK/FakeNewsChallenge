package edu.arizona.cs;

import edu.arizona.cs.data.DataRepo;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;

import java.io.FileNotFoundException;

/**
 * Created by savan on 4/28/17.
 */
public class Main {

    private static final String TRAIN_STANCES = "src/main/resources/fnc_2/train_stances_csc483583.csv";
    private static final String TRAIN_BODIES = "src/main/resources/fnc_2/train_bodies.csv";
    private static final String WN_PROPERTIES = "/wn_file_properties.xml";

    private static DataRepo dataRepo;
    private static Dictionary dictionary;

    public static void main(String[] args) {
        try {
            dataRepo = new DataRepo();
            dictionary = Dictionary.getInstance(Main.class.getResourceAsStream(WN_PROPERTIES));
            dataRepo.readData(TRAIN_STANCES, TRAIN_BODIES);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JWNLException e) {
            e.printStackTrace();
        }
    }
}