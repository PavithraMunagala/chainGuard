package com.pavithra.riskanalyzer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskInputDTO {

    private int highCount;
    private int mediumCount;
    private int lowCount;
    private int infoCount;

    private boolean reentrancyDetected;
    private boolean delegatecallDetected;

    private int externalCallCount;

    private int optimizationCount;
    private int microCount;
    private String contractSourceCode;
}