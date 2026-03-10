package com.pavithra.riskanalyzer.dto;

public class SecurityIssue {

    private String check;
    private String impact;
    private String confidence;
    private int count;
    private String description;
    private String explanation;

    public SecurityIssue(
            String check,
            String impact,
            String confidence,
            int count,
            String description,
            String explanation
    ) {
        this.check = check;
        this.impact = impact;
        this.confidence = confidence;
        this.count = count;
        this.description = description;
        this.explanation = explanation;
    }

    public String getCheck() { return check; }
    public String getImpact() { return impact; }
    public String getConfidence() { return confidence; }
    public int getCount() { return count; }
    public String getDescription() { return description; }
    public String getExplanation() { return explanation; }

    public void incrementCount() { this.count++; }
}