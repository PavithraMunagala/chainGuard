package com.pavithra.riskanalyzer.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RiskResultDTO {

    private int finalScore;
    private String riskLevel;
    private String explanation;

    private int gasScore;
    private String gasLevel;
    private List<String> gasSuggestions;

    private List<String> fixSuggestions;

    private ScoreBreakdownDTO scoreBreakdown;
    private double mlRiskScore;
    private int mlImpact;
}