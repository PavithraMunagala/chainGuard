package com.pavithra.riskanalyzer.controller;

import com.pavithra.riskanalyzer.dto.RiskInputDTO;
import com.pavithra.riskanalyzer.dto.RiskResultDTO;
import com.pavithra.riskanalyzer.service.RiskScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskScoringService riskScoringService;

    @PostMapping("/evaluate")
    public RiskResultDTO evaluate(@RequestBody RiskInputDTO input) {
        return riskScoringService.evaluate(input);
    }
}