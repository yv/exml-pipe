package de.versley.exml.annotators.polart;

public class SentimentItem {
    private Polarity polarity;
    private String form;
    private double weight;

    public Polarity getPolarity() {
        return polarity;
    }

    public void setPolarity(Polarity polarity) {
        this.polarity = polarity;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String toString() {
        return String.format("SentimentItem(%s, %s, %s)", form, polarity, weight);
    }
}
