package edu.arizona.cs.utils;

import edu.arizona.cs.data.Document;
import edu.arizona.cs.data.Stance;

import java.util.Map;

/**
 * Created by savan on 5/6/17.
 */
public class Scorer {
    private static Scorer ourInstance = new Scorer();

    public static Scorer getInstance() {
        return ourInstance;
    }

    private Scorer() {
    }

    public void score(Map<Document, Stance> result) {
        double maxScore = 0, nullScore = 0, testScore = 0;

        for (Stance stance : result.values()) {
            maxScore += getScore(stance, stance);
        }

        for (Map.Entry<Document, Stance> entry : result.entrySet()) {
            testScore += getScore(entry.getKey().getStance(), entry.getValue());
        }

        for(Stance stance : result.values()) {
            nullScore += getScore(new Stance(Stance.STANCE_UNRELATED), stance);
        }

        System.out.println("Max score: " + maxScore);
        System.out.println("Null score: " + nullScore);
        System.out.println("Test score: " + testScore);

        int unrealtedUnrelated = 0;
        int unrelatedDiscuss = 0;
        int unrelatedAgree = 0;
        int unrelatedDisagree = 0;

        int discussUnrelated = 0;
        int discussDiscuss = 0;
        int discussAgree = 0;
        int discussDisagree = 0;

        int agreeUnrelated = 0;
        int agreeDiscuss = 0;
        int agreeAgree = 0;
        int agreeDisagree = 0;

        int disagreeUnrelated = 0;
        int disagreeDiscuss = 0;
        int disagreeAgree = 0;
        int disagreeDisagree = 0;

        for (Map.Entry<Document, Stance> entry : result.entrySet()) {
            Stance predictedStance = entry.getKey().getStance();
            Stance actualStance = entry.getValue();

            if(predictedStance.getStance() == Stance.STANCE_UNRELATED) {
                switch (actualStance.getStance()) {
                    case Stance.STANCE_UNRELATED:
                        unrealtedUnrelated++;
                        break;

                    case Stance.STANCE_AGREE:
                        unrelatedAgree++;
                        break;

                    case Stance.STANCE_DISAGREE:
                        unrelatedDisagree++;
                        break;

                    case Stance.STANCE_DISCUSS:
                        unrelatedDiscuss++;
                        break;
                }
            } else if(predictedStance.getStance() == Stance.STANCE_AGREE) {
                switch (actualStance.getStance()) {
                    case Stance.STANCE_UNRELATED:
                        agreeUnrelated++;
                        break;

                    case Stance.STANCE_AGREE:
                        agreeAgree++;
                        break;

                    case Stance.STANCE_DISAGREE:
                        agreeDisagree++;
                        break;

                    case Stance.STANCE_DISCUSS:
                        agreeDiscuss++;
                        break;
                }
            } else if(predictedStance.getStance() == Stance.STANCE_DISAGREE) {
                switch (actualStance.getStance()) {
                    case Stance.STANCE_UNRELATED:
                        disagreeUnrelated++;
                        break;

                    case Stance.STANCE_AGREE:
                        disagreeAgree++;
                        break;

                    case Stance.STANCE_DISAGREE:
                        disagreeDisagree++;
                        break;

                    case Stance.STANCE_DISCUSS:
                        disagreeDiscuss++;
                        break;
                }
            } else {
                switch (actualStance.getStance()) {
                    case Stance.STANCE_UNRELATED:
                        discussUnrelated++;
                        break;

                    case Stance.STANCE_AGREE:
                        discussAgree++;
                        break;

                    case Stance.STANCE_DISAGREE:
                        discussDisagree++;
                        break;

                    case Stance.STANCE_DISCUSS:
                        discussDiscuss++;
                        break;
                }
            }
        }

        System.out.println("Confusion matrix: ");
        System.out.println("\t\t\t\tActual");
        System.out.println("\t\t\t\t\tUnrelated\tDiscuss\tAgree\tDisagree");
        System.out.print("Predicted");
        System.out.println("\t\tUnrelated\t\t" + unrealtedUnrelated + "\t\t" + unrelatedDiscuss + "\t\t" + unrelatedAgree + "\t\t" + unrelatedDisagree);
        System.out.println("\t\t\t\tDiscuss\t\t\t" + discussUnrelated + "\t\t" + discussDiscuss + "\t\t" + discussAgree + "\t\t" + discussDisagree);
        System.out.println("\t\t\t\tAgree\t\t\t" + agreeUnrelated + "\t\t" + agreeDiscuss + "\t\t" + agreeAgree + "\t\t" + agreeDisagree);
        System.out.println("\t\t\t\tDisagree\t\t" + disagreeUnrelated + "\t\t" + disagreeDiscuss + "\t\t" + disagreeAgree + "\t\t" + disagreeDisagree);
    }

    private double getScore(Stance predictedStance, Stance actualStance) {
        double score = 0;
        if (actualStance.getStance() == Stance.STANCE_UNRELATED) {
            if(predictedStance.getStance() == Stance.STANCE_UNRELATED) {
                score = 0.25;
            } else {
                score = 0;
            }
        } else {
            // if actually related
            if(predictedStance.getStance() != Stance.STANCE_UNRELATED) {
                score = 0.25;
            }

            if(predictedStance.getStance() == actualStance.getStance()) {
                score += 0.5;
            }
        }
        return score;
    }
}