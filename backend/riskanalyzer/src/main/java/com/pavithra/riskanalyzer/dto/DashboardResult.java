package com.pavithra.riskanalyzer.dto;

import java.util.List;

public class DashboardResult {

    private int totalIssues;
    private int informational;
    private int optimization;
    private int low;
    private int medium;
    private int high;

    private List<SecurityIssue> issues;

    public DashboardResult(
            int totalIssues,
            int informational,
            int optimization,
            int low,
            int medium,
            int high,
            List<SecurityIssue> issues
    ) {
        this.totalIssues = totalIssues;
        this.informational = informational;
        this.optimization = optimization;
        this.low = low;
        this.medium = medium;
        this.high = high;
        this.issues = issues;
    }

    public int getTotalIssues() { return totalIssues; }
    public int getInformational() { return informational; }
    public int getOptimization() { return optimization; }
    public int getLow() { return low; }
    public int getMedium() { return medium; }
    public int getHigh() { return high; }
    public List<SecurityIssue> getIssues() { return issues; }
}