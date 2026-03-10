package com.pavithra.riskanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.pavithra.riskanalyzer.dto.MLResultDTO;        // ← ADD THIS TOO

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Service
public class MLAnalysisService {

    private static final String ML_URL = "http://localhost:8000/predict";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public double getMLProbability(String sourceCode) {

        if (sourceCode == null || sourceCode.isBlank()) {
            return 0.0;
        }

        try {

            // Build correct JSON
            String json = objectMapper.writeValueAsString(
                    Map.of("source_code", sourceCode)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ML_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return 0.0;
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (root.has("ml_probability")) {
                return root.get("ml_probability").asDouble();
            }

            return 0.0;

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }
    public MLResultDTO analyze(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return MLResultDTO.builder()
                    .mlProbability(0.0)
                    .mlPrediction(0)
                    .finalPrediction(0)
                    .verdict("CLEAN")
                    .confidenceLevel("Not Analyzed")
                    .confidenceExplanation("No source code provided.")
                    .ruleFlags(new ArrayList<>())
                    .ruleFlagExplanations(new ArrayList<>())
                    .mlImpact(0)
                    .build();
        }

        try {
            String json = objectMapper.writeValueAsString(
                    Map.of("source_code", sourceCode)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ML_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());

            // ── Extract all fields ────────────────────────────────────────────
            double prob            = root.path("ml_probability").asDouble();
            int    mlPrediction    = root.path("ml_prediction").asInt();
            int    finalPrediction = root.path("final_prediction").asInt();
            String verdict         = root.path("verdict").asText("CLEAN");
            String confLevel       = root.path("confidence_level").asText();
            String confExplain     = root.path("confidence_explanation").asText();
            String summary         = root.path("summary").asText();
            String mlImpactExplain = root.path("ml_impact_explanation").asText();
            int    mlImpact        = root.path("ml_impact").asInt();

            // ── Extract rule_names list ───────────────────────────────────────
            ArrayList<String> ruleNames = new ArrayList<String>();
            if (root.has("rule_names") && root.get("rule_names").isArray()) {
                for (JsonNode n : root.get("rule_names")) {
                    ruleNames.add(n.asText());
                }
            }

            // ── Extract rule_descriptions list ────────────────────────────────
            ArrayList<String> ruleDescriptions = new ArrayList<String>();
            if (root.has("rule_descriptions") && root.get("rule_descriptions").isArray()) {
                for (JsonNode n : root.get("rule_descriptions")) {
                    ruleDescriptions.add(n.asText());
                }
            }
            return MLResultDTO.builder()
                    .mlProbability(prob)
                    .mlPrediction(mlPrediction)
                    .finalPrediction(finalPrediction)
                    .verdict(verdict)
                    .confidenceLevel(confLevel)
                    .confidenceExplanation(confExplain)
                    .ruleFlags(ruleNames)
                    .ruleFlagExplanations(ruleDescriptions)
                    .mlImpact(mlImpact)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return MLResultDTO.builder()
                    .mlProbability(0.0)
                    .mlPrediction(0)
                    .finalPrediction(0)
                    .verdict("UNKNOWN")
                    .confidenceLevel("Service Unavailable")
                    .confidenceExplanation("ML service error: " + e.getMessage())
                    .ruleFlags(new ArrayList<>())
                    .ruleFlagExplanations(new ArrayList<>())
                    .mlImpact(0)
                    .build();
        }
    }
    private String classifyConfidence(double prob) {
        if (prob < 0.30) return "Low Suspicion";
        if (prob < 0.50) return "Moderate Suspicion";
        if (prob < 0.75) return "High Suspicion";
        if (prob < 0.90) return "Very High Suspicion";
        return "Extremely High Suspicion";
    }

    private String explainConfidence(double prob) {
        if (prob < 0.30)
            return String.format("The AI model found no significant patterns matching known "
                    + "vulnerable contracts (probability: %.1f%%). Manual review still recommended.", prob * 100);
        if (prob < 0.50)
            return String.format("The AI model detected some patterns resembling vulnerable "
                    + "contracts (probability: %.1f%%). Above the 30%% detection threshold. "
                    + "Manual review recommended.", prob * 100);
        if (prob < 0.75)
            return String.format("The AI model is fairly confident this contract contains "
                    + "vulnerability patterns (probability: %.1f%%). Fix identified issues "
                    + "before any deployment.", prob * 100);
        if (prob < 0.90)
            return String.format("The AI model is highly confident this contract is vulnerable "
                    + "(probability: %.1f%%). Do not deploy without a full security audit.", prob * 100);
        return String.format("The AI model is extremely confident this contract contains "
                + "critical vulnerabilities (probability: %.1f%%). "
                + "Do NOT deploy under any circumstances.", prob * 100);
    }
    public int computeMLImpact(double prob) {

        if (prob > 0.90) return 20;
        if (prob > 0.75) return 12;
        if (prob > 0.60) return 6;

        return 0;
    }
}