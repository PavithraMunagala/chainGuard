package com.pavithra.riskanalyzer.controller;

import com.pavithra.riskanalyzer.dto.*;
import com.pavithra.riskanalyzer.entity.ContractEntity;
import com.pavithra.riskanalyzer.repository.ContractRepository;
import com.pavithra.riskanalyzer.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.pavithra.riskanalyzer.dto.MLResultDTO;        // ← ADD THIS TOO

import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final EtherscanService etherscanService;
    private final ContractService contractService;
    private final ContractRepository contractRepository;
    private final SlitherService slitherService;
    private final RiskScoringService riskScoringService;
    private final SlitherMappingService slitherMappingService;
    private final MicroVulnerabilityService microVulnerabilityService;
    private final MLAnalysisService mlAnalysisService;

    public ContractController(
            EtherscanService etherscanService,
            ContractService contractService,
            ContractRepository contractRepository,
            SlitherService slitherService,
            RiskScoringService riskScoringService,
            SlitherMappingService slitherMappingService,
            MicroVulnerabilityService microVulnerabilityService,
            MLAnalysisService mlAnalysisService                        // ← ADD
    ) {
        this.etherscanService = etherscanService;
        this.contractService = contractService;
        this.contractRepository = contractRepository;
        this.slitherService = slitherService;
        this.riskScoringService = riskScoringService;
        this.slitherMappingService = slitherMappingService;
        this.microVulnerabilityService = microVulnerabilityService;
        this.mlAnalysisService = mlAnalysisService;                // ← ADD
    }

    // =========================================
    // 1️⃣ Analyze by Contract Address
    // =========================================
    @PostMapping("/address")
    public Object submitContractAddress(@RequestBody Map<String, String> body) {

        String address = body.get("address");

        if (address == null || !address.matches("^0x[a-fA-F0-9]{40}$")) {
            return Map.of(
                    "status", "ERROR",
                    "message", "Invalid Ethereum contract address"
            );
        }

        try {

            // Create scan record
            ContractEntity contract = contractService.createScan(address);

            // Fetch source from Etherscan
            contract = contractService.fetchAndPersist(contract);

            if (contract.isSourceAvailable() && contract.getSourceCode() != null) {

                // SLITHER ANALYSIS
                DashboardResult dashboard =
                        slitherService.analyze(contract.getSourceCode());

                // RISK ENGINE
                RiskInputDTO input =
                        slitherMappingService.mapDashboardToRiskInput(dashboard);

                var microFindings =
                        microVulnerabilityService.scan(contract.getSourceCode());

                input = RiskInputDTO.builder()
                        .highCount(input.getHighCount())
                        .mediumCount(input.getMediumCount())
                        .lowCount(input.getLowCount())
                        .infoCount(input.getInfoCount())
                        .optimizationCount(input.getOptimizationCount())
                        .reentrancyDetected(input.isReentrancyDetected())
                        .delegatecallDetected(input.isDelegatecallDetected())
                        .externalCallCount(input.getExternalCallCount())
                        .microCount(microFindings.size())
                        .contractSourceCode(contract.getSourceCode())
                        .build();

                RiskResultDTO riskResult = riskScoringService.evaluate(input);

                MLResultDTO mlResult = mlAnalysisService.analyze(contract.getSourceCode());

                contract.setScanStatus("ANALYZED");
                contractRepository.save(contract);

                return Map.of(
                        "status", "SUCCESS",
                        "contractId", contract.getContractId(),
                        "address", address,
                        "scanStatus", "ANALYZED",
                        "dashboard", dashboard,
                        "riskAnalysis", riskResult,
                        "microVulnerabilities", microFindings,
                        "mlResult", mlResult
                );
            }

            return Map.of(
                    "status", "SUCCESS",
                    "contractId", contract.getContractId(),
                    "address", address,
                    "scanStatus", contract.getScanStatus(),
                    "sourceAvailable", contract.isSourceAvailable(),
                    "slitherExecuted", false
            );

        } catch (Exception e) {
            return Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            );
        }
    }

    // =========================================
    // 2️⃣ Get Scan by ID
    // =========================================
    @GetMapping("/{contractId}")
    public ContractEntity getScan(@PathVariable UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Scan not found"));
    }

    // =========================================
    // 3️⃣ Re-run Risk Analysis
    // =========================================
    @PostMapping("/analyze/{contractId}")
    public RiskResultDTO analyze(@PathVariable UUID contractId) {

        try {

            ContractEntity contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Scan not found"));

            DashboardResult dashboard =
                    slitherService.analyze(contract.getSourceCode());

            RiskInputDTO baseInput =
                    slitherMappingService.mapDashboardToRiskInput(dashboard);

            RiskInputDTO input = RiskInputDTO.builder()
                    .highCount(baseInput.getHighCount())
                    .mediumCount(baseInput.getMediumCount())
                    .lowCount(baseInput.getLowCount())
                    .infoCount(baseInput.getInfoCount())
                    .optimizationCount(baseInput.getOptimizationCount())
                    .reentrancyDetected(baseInput.isReentrancyDetected())
                    .delegatecallDetected(baseInput.isDelegatecallDetected())
                    .externalCallCount(baseInput.getExternalCallCount())
                    .microCount(0)
                    .contractSourceCode(contract.getSourceCode())
                    .build();

            return riskScoringService.evaluate(input);

        } catch (Exception e) {
            throw new RuntimeException("Risk analysis failed: " + e.getMessage());
        }
    }

    // =========================================
    // 4️⃣ Upload Solidity File
    // =========================================
    @PostMapping("/upload")
    public Object uploadContract(@RequestParam("file") MultipartFile file) {

        try {

            String sourceCode = new String(file.getBytes());

            DashboardResult dashboard =
                    slitherService.analyze(sourceCode);

            RiskInputDTO input =
                    slitherMappingService.mapDashboardToRiskInput(dashboard);

            var microFindings =
                    microVulnerabilityService.scan(sourceCode);

            input = RiskInputDTO.builder()
                    .highCount(input.getHighCount())
                    .mediumCount(input.getMediumCount())
                    .lowCount(input.getLowCount())
                    .infoCount(input.getInfoCount())
                    .optimizationCount(input.getOptimizationCount())
                    .reentrancyDetected(input.isReentrancyDetected())
                    .delegatecallDetected(input.isDelegatecallDetected())
                    .externalCallCount(input.getExternalCallCount())
                    .microCount(microFindings.size())
                    .contractSourceCode(sourceCode)
                    .build();

            RiskResultDTO riskResult = riskScoringService.evaluate(input);

            MLResultDTO mlResult = mlAnalysisService.analyze(sourceCode);

            return Map.of(
                    "status", "SUCCESS",
                    "dashboard", dashboard,
                    "riskAnalysis", riskResult,
                    "microVulnerabilities", microFindings,
                    "mlResult", mlResult
            );

        } catch (Exception e) {
            return Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            );
        }
    }
}