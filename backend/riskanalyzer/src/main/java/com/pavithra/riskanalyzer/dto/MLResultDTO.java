package com.pavithra.riskanalyzer.dto;

import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;          // ← must be java.util.List not anything else

@Data
@Builder
public class MLResultDTO {
    private double mlProbability;
    private int    mlPrediction;
    private int    finalPrediction;
    private String verdict;
    private String confidenceLevel;
    private String confidenceExplanation;
    private List<String> ruleFlags;            // ← List<String> not List<Object>
    private List<String> ruleFlagExplanations; // ← List<String> not List<Object>
    private int    mlImpact;
    private String summary;
    private String mlImpactExplanation;
}