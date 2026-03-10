package com.pavithra.riskanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavithra.riskanalyzer.entity.SlitherFinding;
import com.pavithra.riskanalyzer.repository.SlitherFindingRepository;
import org.springframework.stereotype.Service;
import com.pavithra.riskanalyzer.dto.DashboardResult;
import com.pavithra.riskanalyzer.dto.SecurityIssue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;
import java.util.UUID;
import java.util.regex.*;

@Service
public class SlitherService {

    private final SlitherFindingRepository findingRepository;

    public SlitherService(SlitherFindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    // 1️⃣ Save Solidity file
    public String saveContract(String solidityCode, UUID contractId) throws Exception {

        Path directory = Paths.get("contracts");

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path filePath = directory.resolve(contractId + ".sol");
        Files.write(filePath, solidityCode.getBytes());

        return filePath.toAbsolutePath().toString();
    }

    // 2️⃣ Convert Windows → WSL path
    public String convertToWslPath(String windowsPath) {

        String driveLetter = windowsPath.substring(0, 1).toLowerCase();
        String pathWithoutDrive = windowsPath.substring(2).replace("\\", "/");

        return "/mnt/" + driveLetter + pathWithoutDrive;
    }

    // 3️⃣ Detect Solidity Version From Pragma (Simple + Safe)
    private String detectSolcVersion(String sourceCode) {

        Pattern pattern = Pattern.compile("pragma\\s+solidity\\s+([^;]+);");
        Matcher matcher = pattern.matcher(sourceCode);

        if (!matcher.find()) {
            return "0.8.20"; // fallback
        }

        String pragma = matcher.group(1).trim();

        if (pragma.contains("0.4")) return "0.4.25";
        if (pragma.contains("0.5")) return "0.5.17";
        if (pragma.contains("0.6")) return "0.6.12";
        if (pragma.contains("0.7")) return "0.7.6";
        if (pragma.contains("0.8")) return "0.8.20";

        return "0.8.20";
    }

    // 4️⃣ Switch Compiler Inside WSL
    private void switchCompiler(String version) throws Exception {

        ProcessBuilder builder = new ProcessBuilder(
                "wsl",
                "-d",
                "Ubuntu",
                "bash",
                "-lc",
                "solc-select use " + version
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();
        process.waitFor();

        System.out.println("Switched solc to: " + version);
    }

    // 5️⃣ Execute Slither
    private String executeSlither(String wslPath, String sourceCode) throws Exception {

        String version = detectSolcVersion(sourceCode);

        String solcPath;

        switch (version) {
            case "0.4.25":
                solcPath = "/home/pavit/solc-bin/solc-0.4.25";
                break;
            case "0.5.17":
                solcPath = "/home/pavit/solc-bin/solc-0.5.17";
                break;
            case "0.8.20":
            default:
                solcPath = "/home/pavit/solc-bin/solc-0.8.20";
                break;
        }

        ProcessBuilder builder = new ProcessBuilder(
                "wsl",
                "-d",
                "Ubuntu",
                "bash",
                "-lc",
                "slither " + wslPath + " --solc " + solcPath + " --json -"
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        String result = output.toString();

        if (result == null || result.isBlank()) {
            throw new RuntimeException("Slither returned empty output.");
        }
        return result;
    }

    // 6️⃣ Execute Advisory Script
    public String executeAdvisory(String wslPath) throws Exception {

        ProcessBuilder builder = new ProcessBuilder(
                "wsl",
                "-d",
                "Ubuntu",
                "bash",
                "-lc",
                "python3 /mnt/c/Users/pavit/OneDrive/Documents/Literature\\ survey/riskanalyzer/scripts/advisory_engine.py " + wslPath
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        process.waitFor();
        return output.toString();
    }

    // 7️⃣ Save Raw Result
    public void saveRawResult(String json, String advisoryOutput, UUID contractId) {

        SlitherFinding finding = new SlitherFinding();
        finding.setContractId(contractId);
        finding.setRawJson(json);
        finding.setAdvisoryJson(advisoryOutput);

        findingRepository.save(finding);
    }

    // 8️⃣ Full Pipeline (Auto Multi-Pragma Enabled)
    public DashboardResult analyze(String solidityCode) throws Exception {

        UUID contractId = UUID.randomUUID();

        String windowsPath = saveContract(solidityCode, contractId);
        String wslPath = convertToWslPath(windowsPath);

        // 🔥 Detect + Switch compiler BEFORE Slither
        String version = detectSolcVersion(solidityCode);
        switchCompiler(version);

        String slitherJson = executeSlither(wslPath, solidityCode);
        String advisoryOutput = executeAdvisory(wslPath);

        saveRawResult(slitherJson, advisoryOutput, contractId);

        return buildDashboard(slitherJson);
    }

    // 9️⃣ Build Dashboard
    private DashboardResult buildDashboard(String slitherJson) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode detectors = mapper.readTree(slitherJson)
                .path("results")
                .path("detectors");

        Map<String, SecurityIssue> grouped = new HashMap<>();

        int informational = 0;
        int optimization = 0;
        int low = 0;
        int medium = 0;
        int high = 0;

        for (JsonNode detector : detectors) {

            String check = detector.path("check").asText();
            String impact = detector.path("impact").asText();
            String confidence = detector.path("confidence").asText();
            String description = detector.path("description").asText();

            switch (impact.toLowerCase()) {
                case "informational": informational++; break;
                case "optimization": optimization++; break;
                case "low": low++; break;
                case "medium": medium++; break;
                case "high": high++; break;
            }

            if (!grouped.containsKey(check)) {

                grouped.put(check,
                        new SecurityIssue(
                                check,
                                impact,
                                confidence,
                                1,
                                description,
                                explain(check)
                        )
                );

            } else {
                grouped.get(check).incrementCount();
            }
        }

        List<SecurityIssue> issues = new ArrayList<>(grouped.values());

        int totalIssues = informational + optimization + low + medium + high;

        return new DashboardResult(
                totalIssues,
                informational,
                optimization,
                low,
                medium,
                high,
                issues
        );
    }

    private String explain(String check) {
        switch (check) {

            // ── HIGH SEVERITY ──────────────────────────────────────────
            case "reentrancy-eth":
            case "reentrancy-unlimited-gas":
            case "reentrancy-no-eth":
            case "reentrancy-benign":
                return "Reentrancy vulnerability detected. An external call " +
                        "is made before state variables are updated. An attacker " +
                        "can exploit this to repeatedly drain funds. Fix: Apply " +
                        "Checks-Effects-Interactions pattern — update state BEFORE " +
                        "making external calls.";

            case "suicidal":
            case "controlled-delegatecall":
                return "Anyone can destroy or hijack this contract. A function " +
                        "allows unauthorized users to call selfdestruct or " +
                        "delegatecall with user-controlled input. Fix: Add " +
                        "access control (onlyOwner modifier).";

            case "arbitrary-send-eth":
            case "arbitrary-send-erc20":
                return "Arbitrary ETH/token transfer vulnerability. An attacker " +
                        "can redirect funds to any address. Fix: Validate the " +
                        "recipient address and restrict who can call this function.";

            case "controlled-array-length":
                return "Array length is controlled by user input. This can be " +
                        "exploited to overwrite storage. Fix: Never set array " +
                        "length from untrusted input.";

            case "msg-value-loop":
                return "msg.value is used inside a loop. This is dangerous in " +
                        "batch operations and can lead to unexpected behavior. " +
                        "Fix: Avoid using msg.value inside loops.";

            // ── MEDIUM SEVERITY ────────────────────────────────────────
            case "tx-origin":
                return "tx.origin is used for authentication. A malicious contract " +
                        "can trick a legitimate user into calling it, bypassing this " +
                        "check. Fix: Replace tx.origin with msg.sender for all " +
                        "authorization checks.";

            case "divide-before-multiply":
                return "Division before multiplication causes precision loss due " +
                        "to integer truncation. Fix: Always multiply before dividing " +
                        "to preserve precision.";

            case "weak-prng":
                return "Weak randomness detected. block.timestamp or blockhash " +
                        "are used as random sources, which miners can manipulate. " +
                        "Fix: Use Chainlink VRF or commit-reveal scheme for randomness.";

            case "unchecked-transfer":
            case "unchecked-send":
            case "unchecked-lowlevel":
                return "Return value of transfer/send/low-level call is not checked. " +
                        "If the call fails silently, the contract continues as if " +
                        "it succeeded. Fix: Always check return values with require().";

            case "locked-ether":
                return "Contract can receive ETH but has no way to withdraw it. " +
                        "Funds will be permanently locked. Fix: Add a withdrawal " +
                        "function or payable fallback.";

            // ── LOW SEVERITY ───────────────────────────────────────────
            case "low-level-calls":
                return "Low-level call (.call, .delegatecall, .staticcall) detected. " +
                        "These bypass type checking and are risky if used incorrectly. " +
                        "Fix: Use high-level calls where possible and always check " +
                        "return values.";

            case "calls-loop":
                return "External calls inside a loop can hit the gas limit and cause " +
                        "denial of service. Fix: Use the pull-payment pattern instead " +
                        "of pushing payments in loops.";

            case "tautology":
                return "A condition is always true or always false (tautology). " +
                        "This suggests a logic error. Fix: Review the condition logic.";

            case "boolean-equality":
                return "Comparing a boolean to true/false explicitly is redundant " +
                        "and wastes gas. Fix: Use the boolean directly (if (flag) " +
                        "instead of if (flag == true)).";

            case "shadowing-local":
            case "shadowing-state":
            case "shadowing-builtin":
                return "A variable name shadows an outer scope variable or built-in. " +
                        "This causes confusion and potential bugs. Fix: Rename the " +
                        "variable to avoid shadowing.";

            // ── INFORMATIONAL ──────────────────────────────────────────
            case "solc-version":
                return "Outdated Solidity compiler version detected. Versions below " +
                        "0.8.0 lack built-in overflow protection and other safety " +
                        "features. Fix: Upgrade to Solidity 0.8.x or higher.";

            case "pragma":
                return "Floating pragma detected (^0.x.x). This allows compilation " +
                        "with different compiler versions, which may introduce bugs. " +
                        "Fix: Lock the pragma to a specific version (=0.8.20).";

            case "naming-convention":
                return "Variable or function name does not follow Solidity naming " +
                        "conventions. This reduces code readability. Fix: Use " +
                        "camelCase for functions and mixedCase for variables.";

            case "dead-code":
                return "Unreachable code detected. This code will never execute " +
                        "and wastes deployment gas. Fix: Remove the dead code.";

            case "unused-return":
                return "Return value of a function call is ignored. This may " +
                        "indicate a logic error. Fix: Store and check the return value.";

            // ── OPTIMIZATION ───────────────────────────────────────────
            case "constable-states":
                return "State variable can be declared constant but is not. " +
                        "Constant variables are cheaper to access. Fix: Add the " +
                        "constant keyword to this variable.";

            case "immutable-states":
                return "State variable is set only in the constructor and never " +
                        "changed. Declaring it immutable saves gas on reads. " +
                        "Fix: Replace state with immutable keyword.";

            case "var-read-using-this":
                return "State variable is read using this.variable instead of " +
                        "directly. This makes an external call and wastes gas. " +
                        "Fix: Access the variable directly without this.";

            default:
                return "Potential security issue detected by Slither static analysis. " +
                        "Check: " + check + ". Review the code carefully and consult " +
                        "the SWC Registry (swcregistry.io) for remediation guidance.";
        }
    }
}