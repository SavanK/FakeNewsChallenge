package edu.arizona.cs.data;

/**
 * Created by savan on 4/28/17.
 */
public class Stance {
    public static final int STANCE_DISCUSS = 0;
    public static final int STANCE_AGREE = 1;
    public static final int STANCE_DISAGREE = 2;
    public static final int STANCE_UNRELATED = 3;

    public static final String STANCE_STR_DISCUSS = "discuss";
    public static final String STANCE_STR_AGREE = "agree";
    public static final String STANCE_STR_DISAGREE = "disagree";
    public static final String STANCE_STR_UNRELATED = "unrelated";

    private int stance;

    public Stance(int stance) {
        this.stance = stance;
    }

    public int getStance() {
        return stance;
    }

    public String getStanceStr() {
        String stanceStr = "";
        switch (stance) {
            case STANCE_AGREE:
                stanceStr = STANCE_STR_AGREE;
                break;

            case STANCE_DISAGREE:
                stanceStr = STANCE_STR_DISAGREE;
                break;

            case STANCE_DISCUSS:
                stanceStr = STANCE_STR_DISCUSS;
                break;

            case STANCE_UNRELATED:
                stanceStr = STANCE_STR_UNRELATED;
                break;
        }
        return stanceStr;
    }

    public static int getStanceID(String stanceStr) {
        int stance = STANCE_UNRELATED;
        if(stanceStr.compareToIgnoreCase(STANCE_STR_AGREE) == 0) {
            stance = STANCE_AGREE;
        } else if(stanceStr.compareToIgnoreCase(STANCE_STR_DISAGREE) == 0) {
            stance = STANCE_DISAGREE;
        } else if(stanceStr.compareToIgnoreCase(STANCE_STR_DISCUSS) == 0) {
            stance = STANCE_DISCUSS;
        } else if(stanceStr.compareToIgnoreCase(STANCE_STR_UNRELATED) == 0) {
            stance = STANCE_UNRELATED;
        }
        return stance;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Stance) {
            return stance == ((Stance) obj).getStance();
        } else {
            return false;
        }
    }
}
