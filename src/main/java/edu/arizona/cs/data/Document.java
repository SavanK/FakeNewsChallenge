package edu.arizona.cs.data;

/**
 * Created by savan on 4/28/17.
 */
public class Document {
    private Headline headline;
    private int bodyId;
    private Stance stance;

    public Document(Headline headline, int bodyId, Stance stance) {
        this.headline = headline;
        this.bodyId = bodyId;
        this.stance = stance;
    }

    public Headline getHeadline() {
        return headline;
    }

    public int getBodyId() {
        return bodyId;
    }

    public Stance getStance() {
        return stance;
    }
}
