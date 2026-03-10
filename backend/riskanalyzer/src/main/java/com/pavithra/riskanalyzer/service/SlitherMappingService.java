package com.pavithra.riskanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavithra.riskanalyzer.dto.DashboardResult;
import com.pavithra.riskanalyzer.dto.RiskInputDTO;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class SlitherMappingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // OLD FILE-BASED METHOD (optional, can delete later)
    public RiskInputDTO mapSlitherJsonToRiskInput(String jsonFilePath) throws IOException {

        JsonNode root = objectMapper.readTree(new File(jsonFilePath));
        JsonNode detectors = root.path("results").path("detectors");

        int high = 0;
        int medium = 0;
        int low = 0;
        int info = 0;

        boolean reentrancyDetected = false;
        boolean delegatecallDetected = false;
        int externalCallCount = 0;

        for (JsonNode detector : detectors) {

            String impact = detector.path("impact").asText();
            String check = detector.path("check").asText();

            switch (impact) {
                case "High" -> high++;
                case "Medium" -> medium++;
                case "Low" -> low++;
                case "Informational" -> info++;
            }

            if (check.contains("reentrancy")) {
                reentrancyDetected = true;
            }

            if (check.contains("delegatecall")) {
                delegatecallDetected = true;
            }

            if (check.contains("external")) {
                externalCallCount++;
            }
        }

        return RiskInputDTO.builder()
                .highCount(high)
                .mediumCount(medium)
                .lowCount(low)
                .infoCount(info)
                .reentrancyDetected(reentrancyDetected)
                .delegatecallDetected(delegatecallDetected)
                .externalCallCount(externalCallCount)
                .build();
    }

    // NEW DASHBOARD-BASED METHOD (THIS IS WHAT YOU USE NOW)
    public RiskInputDTO mapDashboardToRiskInput(DashboardResult dashboard) {
        System.out.println("DASH OPT: " + dashboard.getOptimization());
        int high = dashboard.getHigh();
        int medium = dashboard.getMedium();
        int low = dashboard.getLow();
        int info = dashboard.getInformational();
        int optimization = dashboard.getOptimization();
        int optimizationCount = dashboard.getOptimization();

        boolean reentrancyDetected = false;
        boolean delegatecallDetected = false;
        int externalCallCount = 0;

        if (dashboard.getIssues() != null) {
            for (var issue : dashboard.getIssues()) {

                String check = issue.getCheck().toLowerCase();
                String description = issue.getDescription().toLowerCase();

                if (check.contains("reentrancy")) {
                    reentrancyDetected = true;
                }

                if (check.contains("delegatecall")) {
                    delegatecallDetected = true;
                }

                if (description.contains("external call")
                        || description.contains(".call")
                        || description.contains("transfer(")
                        || description.contains("send(")) {

                    externalCallCount += issue.getCount();
                }
            }
        }

        return RiskInputDTO.builder()
                .highCount(high)
                .mediumCount(medium)
                .lowCount(low)
                .infoCount(info)
                .optimizationCount(optimization)
                .reentrancyDetected(reentrancyDetected)
                .delegatecallDetected(delegatecallDetected)
                .externalCallCount(externalCallCount)
                .build();
    }
}