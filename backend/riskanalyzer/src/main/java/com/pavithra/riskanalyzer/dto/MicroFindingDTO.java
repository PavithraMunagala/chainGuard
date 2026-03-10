package com.pavithra.riskanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MicroFindingDTO {

    private String type;        // ex: TX_ORIGIN_USAGE
    private String severity;    // LOW / MEDIUM / HIGH
    private String description; // short explanation
}