package edu.arizona.cs.data;

import net.sf.extjwnl.dictionary.Dictionary;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Created by savan on 4/28/17.
 */
public class DataRepo {

    public static final String BODY_ID = "Body ID";
    public static final String ARTICLE_BODY = "articleBody";
    public static final String HEADLINE = "Headline";
    public static final String STANCE = "Stance";

    private Map<Integer, Body> bodies;
    private List<Document> documents;
    private Dictionary dictionary;

    public DataRepo(Dictionary dictionary) {
        this.dictionary = dictionary;
        bodies = new HashMap<Integer, Body>();
        documents = new ArrayList<Document>();
    }

    public Map<Integer, Body> getBodies() {
        return bodies;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void readData(String trainStancesPath, String trainBodiesPath) throws FileNotFoundException {
        Reader in = new FileReader(trainBodiesPath);
        try {
            CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader());
            for (CSVRecord record : parser) {
                int bodyId = Integer.parseInt(record.get(BODY_ID));
                String bodyText = record.get(ARTICLE_BODY);

                Body body = new Body(bodyText);
                bodies.put(bodyId, body);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        in = new FileReader(trainStancesPath);
        try {
            CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader());
            for (CSVRecord record : parser) {
                int bodyId = Integer.parseInt(record.get(BODY_ID));
                String headlineStr = record.get(HEADLINE);
                String stanceStr = record.get(STANCE);

                Document document = new Document(
                        new Headline(headlineStr),
                        bodyId,
                        new Stance(Stance.getStanceID(stanceStr)));
                documents.add(document);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
