package com.pavithra.riskanalyzer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreBreakdownDTO {

    private int baseScore;
    private int externalCallImpact;
    private int reentrancyImpact;
    private int delegatecallImpact;
    private int finalComputedScore;
    private int microImpact;
    private int mlImpact;
}