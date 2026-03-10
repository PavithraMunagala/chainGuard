package com.pavithra.riskanalyzer.service;

import com.pavithra.riskanalyzer.dto.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RiskScoringService {
    private final MLAnalysisService mlAnalysisService;

    public RiskScoringService(MLAnalysisService mlAnalysisService) {
        this.mlAnalysisService = mlAnalysisService;
    }

    public RiskResultDTO evaluate(RiskInputDTO input) {
        // -----------------------------
        // ML Score
        // -----------------------------
        double mlScore = 0.0;
        if (input.getContractSourceCode() != null) {
            mlScore = mlAnalysisService.getMLProbability(input.getContractSourceCode());
        }

        int mlImpact = calculateMlImpact(mlScore);


        // ── Component Scores ──────────────────────────────────────────────────
        int baseScore        = calculateBaseScore(input);
        int externalImpact   = calculateExternalImpact(baseScore, input);
        int reentrancyImpact = calculateReentrancyImpact(input);
        int delegatecallImpact = calculateDelegatecallImpact(input);
        int microImpact      = input.getMicroCount() >= 2 ? 10 : 0;

        int computedScore = baseScore
                + externalImpact
                + reentrancyImpact
                + delegatecallImpact
                + microImpact
                + mlImpact;

        int finalScore = normalizeScore(computedScore);

        // Structural floors
        if (input.isReentrancyDetected() && finalScore < 40) {
            finalScore = 40;
        }

        if (input.isDelegatecallDetected() && finalScore < 70) {
            finalScore = 70;
        }

        if (input.isReentrancyDetected()
                && input.isDelegatecallDetected()
                && finalScore < 75) {
            finalScore = 75;
        }

        // Gas Layer (separate from security score)
        int gasScore = calculateGasScore(input);
        String gasLevel = classifyGasLevel(gasScore);
        List<String> gasSuggestions = generateGasSuggestions(input);

        // ── Output ────────────────────────────────────────────────────────────
        String level         = classifyRiskLevel(finalScore);
        String explanation   = generateExplanation(input, finalScore);
        List<String> fixes   = generateFixSuggestions(input);

        ScoreBreakdownDTO breakdown = ScoreBreakdownDTO.builder()
                .baseScore(baseScore)
                .externalCallImpact(externalImpact)
                .reentrancyImpact(reentrancyImpact)
                .delegatecallImpact(delegatecallImpact)
                .microImpact(microImpact)
                .mlImpact(mlImpact)
                .finalComputedScore(finalScore)
                .build();

        return RiskResultDTO.builder()
                .finalScore(finalScore)
                .riskLevel(level)
                .explanation(explanation)
                .fixSuggestions(fixes)
                .scoreBreakdown(breakdown)
                .gasScore(gasScore)
                .gasLevel(gasLevel)
                .gasSuggestions(gasSuggestions)
                .mlRiskScore(mlScore)
                .mlImpact(mlImpact)
                .build();
    }
    // ══════════════════════════════════════════════════════════════════════════
    // SCORING COMPONENTS
    // ══════════════════════════════════════════════════════════════════════════

    private int calculateMlImpact(double mlScore) {
        if (mlScore < 0.3) return 0;
        return (int) (mlScore * 20);    // max 20 at probability = 1.0
    }

    private int calculateBaseScore(RiskInputDTO input) {
        return (input.getHighCount()   * 15)
                + (input.getMediumCount() * 6)
                + (input.getLowCount()    * 3)
                + (input.getInfoCount()   * 1);
    }


    // -------------------------------------------------
    // External Call Impact
    // -------------------------------------------------
    private int calculateExternalImpact(int baseScore, RiskInputDTO input) {
        return (input.getExternalCallCount() > 2) ? Math.round(baseScore * 0.25f) : 0;
    }
    // -------------------------------------------------
    // Reentrancy Impact
    // -------------------------------------------------
    private int calculateReentrancyImpact(RiskInputDTO input) {
        if (!input.isReentrancyDetected()) return 0;
        int impact = 20;
        if (input.getExternalCallCount() > 0) impact += input.getExternalCallCount() * 5;
        return impact;
    }
    // -------------------------------------------------
    // Delegatecall Impact
    // -------------------------------------------------
    private int calculateDelegatecallImpact(RiskInputDTO input) {
        if (!input.isDelegatecallDetected()) return 0;
        int impact = 25;
        if (input.isReentrancyDetected()) impact += 15;
        return impact;
    }

    // -------------------------------------------------
    // Normalize
    // -------------------------------------------------
    private int normalizeScore(int score) {
        return Math.min(score, 100);
    }
    // -------------------------------------------------
    // Risk Classification
    // -------------------------------------------------
    private String classifyRiskLevel(int score) {

        if (score <= 15) return "Safe";
        if (score <= 35) return "Low Risk";
        if (score <= 60) return "Moderate Risk";
        if (score <= 80) return "High Risk";
        return "Critical Risk";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HUMAN-READABLE EXPLANATION
    // ══════════════════════════════════════════════════════════════════════════

    private String generateExplanation(RiskInputDTO input, int score) {
        if (input.isReentrancyDetected() && input.isDelegatecallDetected())
            return "Critical structural vulnerability: delegatecall combined with reentrancy " +
                    "creates severe exploit risk. An attacker can potentially take full control " +
                    "of the contract and drain all funds.";

        if (input.isDelegatecallDetected())
            return "Delegatecall usage detected. This allows an external contract to execute " +
                    "code inside your contract's storage context — if the target is malicious " +
                    "or upgradeable, your contract's data can be corrupted or stolen.";

        if (input.isReentrancyDetected()) {
            if (input.getMicroCount() > 0)
                return "Reentrancy vulnerability combined with additional code weaknesses. " +
                        "An attacker can call back into your withdraw/transfer functions " +
                        "before balances are updated, draining funds repeatedly.";
            return "Reentrancy pattern detected. Your contract makes an external call before " +
                    "updating its internal state. This is how the DAO hack stole $60 million — " +
                    "an attacker can repeatedly withdraw funds in a single transaction.";
        }

        if (input.getHighCount() > 0)
            return "Serious security vulnerabilities found that could allow an attacker " +
                    "to steal funds or take control of the contract. Fix these before deployment.";

        if (input.getMediumCount() > 0)
            return "Security issues found that could cause your contract to behave " +
                    "unexpectedly under certain conditions. Review and fix before going live.";

        if (input.getLowCount() > 0)
            return "Minor security issues found. These are unlikely to cause immediate " +
                    "loss but could be exploited in combination with other vulnerabilities.";

        if (input.getMicroCount() > 0)
            return "Code quality issues detected. Your contract uses patterns that are " +
                    "outdated or may behave unexpectedly with future Ethereum network upgrades.";

        if (input.getInfoCount() > 0)
            return "Code style and best practice issues found. No immediate security risk, " +
                    "but fixing these makes your contract easier to audit and maintain.";

        return "No significant issues detected. Your contract follows good security practices.";
    }
    // ══════════════════════════════════════════════════════════════════════════
    // FIX SUGGESTIONS — FULLY HUMAN-READABLE, SPECIFIC TO WHAT IS FOUND
    // ══════════════════════════════════════════════════════════════════════════

    private List<String> generateFixSuggestions(RiskInputDTO input) {

        List<String> fixes = new ArrayList<>();
        String code = input.getContractSourceCode() != null
                ? input.getContractSourceCode().toLowerCase() : "";

        // ── CRITICAL: Reentrancy ──────────────────────────────────────────────
        if (input.isReentrancyDetected()) {
            fixes.add("🔴 CRITICAL — Reentrancy: Move all state changes (like " +
                    "balances[msg.sender] -= amount) ABOVE the external call. " +
                    "The rule is: Check → Update state → Then call external contract.");
            fixes.add("🔴 CRITICAL — Add 'nonReentrant' modifier from OpenZeppelin's " +
                    "ReentrancyGuard to every function that sends ETH or calls external contracts. " +
                    "Import: @openzeppelin/contracts/security/ReentrancyGuard.sol");
        }

        // ── CRITICAL: Delegatecall ────────────────────────────────────────────
        if (input.isDelegatecallDetected()) {
            fixes.add("🔴 CRITICAL — Delegatecall: Never delegatecall to an address " +
                    "that a user can control or change. Store the implementation address " +
                    "as an immutable variable set only in the constructor.");
            if (input.isReentrancyDetected()) {
                fixes.add("🔴 CRITICAL — Reentrancy + Delegatecall together is extremely " +
                        "dangerous. Consider redesigning using OpenZeppelin's transparent " +
                        "proxy pattern which has these protections built in.");
            }
        }

        // ── HIGH: External Calls ──────────────────────────────────────────────
        if (input.getExternalCallCount() > 2) {
            fixes.add("🟠 HIGH — Too many external calls (" + input.getExternalCallCount() +
                    " found): Each external call is a potential attack surface. " +
                    "Use the pull-payment pattern — let users withdraw funds themselves " +
                    "instead of your contract pushing ETH to them in loops.");
        }

        // ── MEDIUM: Specific patterns ─────────────────────────────────────────
        if (input.getMediumCount() > 0) {

            if (code.contains("tx.origin")) {
                fixes.add("🟡 MEDIUM — tx.origin used for authentication: This is dangerous. " +
                        "A malicious contract can trick a user into calling it, and " +
                        "tx.origin will still be the victim's address. " +
                        "Replace every 'tx.origin' with 'msg.sender'.");
            }

            if (code.contains(".send(")) {
                fixes.add("🟡 MEDIUM — Unchecked .send() return value: The .send() function " +
                        "returns false if it fails (it does NOT revert). Your contract " +
                        "continues running even if the ETH transfer failed. Fix: " +
                        "(bool success, ) = addr.call{value: amount}(''); require(success, 'Transfer failed');");
            }

            if (code.contains("block.timestamp") || code.contains("now")) {
                fixes.add("🟡 MEDIUM — Timestamp dependence: Miners can manipulate " +
                        "block.timestamp by up to ~15 seconds. Do not use it for randomness " +
                        "or critical timing logic. Use Chainlink VRF for randomness instead.");
            }

            if (code.contains("block.blockhash") || code.contains("blockhash(")) {
                fixes.add("🟡 MEDIUM — Weak randomness using blockhash: Miners know the " +
                        "blockhash in advance and can manipulate results. Use Chainlink VRF " +
                        "for any randomness that has financial consequences.");
            }

            if (!code.contains("tx.origin") && !code.contains(".send(")
                    && !code.contains("block.timestamp") && !code.contains("blockhash(")) {
                fixes.add("🟡 MEDIUM — Review all medium severity Slither findings. " +
                        "Check for unchecked return values from external calls and " +
                        "ensure all arithmetic operations handle edge cases correctly.");
            }
        }

        // ── LOW: Specific patterns ────────────────────────────────────────────
        if (input.getLowCount() > 0) {

            if (code.contains(".call(") || code.contains(".call.value(")) {
                fixes.add("🔵 LOW — Low-level .call() usage: Always capture and check " +
                        "the return value. Pattern: " +
                        "(bool ok, bytes memory data) = addr.call{value:v}(payload); " +
                        "require(ok, 'Call failed');");
            }

            if (code.contains("for") && (code.contains(".push(") || code.contains("length"))) {
                fixes.add("🔵 LOW — Unbounded loop detected: If the array grows too large, " +
                        "your function will hit the gas limit and permanently fail. " +
                        "Add a maximum array size or use pagination with offsets.");
            }

            if (code.contains("selfdestruct") || code.contains("suicide(")) {
                fixes.add("🔵 LOW — selfdestruct usage: Restrict who can call this with " +
                        "onlyOwner modifier. Unprotected selfdestruct lets anyone destroy " +
                        "your contract and steal its ETH balance.");
            }

            if (!code.contains(".call(") && !code.contains("for")) {
                fixes.add("🔵 LOW — Address low severity issues: remove any unreachable code, " +
                        "replace floating pragma (^0.x.x) with a fixed version (=0.8.20), " +
                        "and always handle return values from external calls.");
            }
        }

        // ── MICRO: Old compiler / transfer patterns ───────────────────────────
        if (input.getMicroCount() > 0) {

            if (code.contains("pragma solidity ^0.4") || code.contains("pragma solidity ^0.5")
                    || code.contains("pragma solidity ^0.6") || code.contains("pragma solidity ^0.7")) {
                fixes.add("⚪ CODE QUALITY — Old Solidity version: Versions below 0.8.0 do NOT " +
                        "automatically check for integer overflow/underflow. A value of 255 + 1 " +
                        "wraps back to 0. Either upgrade to Solidity 0.8.x or import " +
                        "OpenZeppelin SafeMath for all arithmetic.");
            }

            if (code.contains(".transfer(")) {
                fixes.add("⚪ CODE QUALITY — .transfer() has a fixed gas limit of 2,300. " +
                        "Since EIP-1884, some operations cost more gas, causing .transfer() " +
                        "to fail on contracts with complex fallback functions. " +
                        "Replace with: (bool ok,) = recipient.call{value: amount}(''); require(ok);");
            }

            if (code.contains("emit") && code.contains(".transfer(")) {
                fixes.add("⚪ CODE QUALITY — Event emitted after external transfer: " +
                        "If the transfer fails or is exploited, your event log will be " +
                        "misleading. Emit events before making external calls.");
            }
        }

        // ── INFORMATIONAL (explained in plain English) ────────────────────────
        if (input.getInfoCount() > 0) {
            fixes.add("ℹ️ BEST PRACTICE — " + input.getInfoCount() + " code style issue(s) found. " +
                    "These do not cause security risks right now, but they make your contract " +
                    "harder to audit. Common issues: variable names not following conventions, " +
                    "functions not marked as view/pure when they should be, or missing NatSpec comments.");

            if (code.contains("pragma solidity ^") || code.contains("pragma solidity >=")) {
                fixes.add("ℹ️ BEST PRACTICE — Floating pragma detected (^ or >=). " +
                        "This means your contract can compile with different Solidity versions " +
                        "which may behave differently. Lock it to one version: " +
                        "pragma solidity 0.8.20;");
            }
        }

        // ── OPTIMIZATION (explained in plain English) ─────────────────────────
        if (input.getOptimizationCount() > 0) {
            fixes.add("💡 GAS SAVING — " + input.getOptimizationCount() + " gas optimization(s) found. " +
                    "Variables that never change after deployment should be marked 'constant' " +
                    "(for compile-time values) or 'immutable' (for values set in constructor). " +
                    "This saves ~20,000 gas every time the variable is read.");

            if (code.contains("for") || code.contains("while")) {
                fixes.add("💡 GAS SAVING — Storage reads inside loops are expensive (2,100 gas each). " +
                        "Copy array.length and frequently-read storage variables into a local " +
                        "memory variable before the loop starts.");
            }
        }

        // ── ML flagged but no other issues ───────────────────────────────────
        if (input.getMicroCount() == 0 && input.getLowCount() == 0
                && input.getMediumCount() == 0 && input.getHighCount() == 0
                && mlAnalysisService.getMLProbability(
                input.getContractSourceCode() != null
                        ? input.getContractSourceCode() : "") >= 0.3) {
            fixes.add("🤖 ML DETECTION — The AI model flagged this contract as potentially " +
                    "vulnerable (above 0.3 confidence threshold) even though no specific " +
                    "rule matched. This means the code has statistical patterns similar to " +
                    "known vulnerable contracts. Have a security expert manually review it.");
        }

        // ── All clear ─────────────────────────────────────────────────────────
        if (fixes.isEmpty()) {
            fixes.add("✅ No critical issues detected. Before mainnet deployment, " +
                    "still consider: (1) a professional security audit, " +
                    "(2) testing with Foundry or Hardhat, " +
                    "(3) running Echidna fuzzer for edge cases.");
        }

        return fixes;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GAS SCORING — INDEPENDENT LAYER
    // ══════════════════════════════════════════════════════════════════════════

    private int calculateGasScore(RiskInputDTO input) {
        return Math.min(input.getOptimizationCount() * 10, 100);
    }

    private String classifyGasLevel(int score) {
        if (score == 0)   return "Efficient";
        if (score <= 20)  return "Minor Gas Inefficiency";
        if (score <= 50)  return "Moderate Gas Inefficiency";
        return "High Gas Inefficiency";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GAS SUGGESTIONS — MAXIMUM COVERAGE, ALL SPECIFIC
    // ══════════════════════════════════════════════════════════════════════════

    private List<String> generateGasSuggestions(RiskInputDTO input) {

        List<String> suggestions = new ArrayList<>();
        String code = input.getContractSourceCode() != null
                ? input.getContractSourceCode().toLowerCase() : "";

        // 1. Constant / Immutable variables
        if (input.getOptimizationCount() > 0) {
            suggestions.add("💡 Declare state variables that never change as 'constant' " +
                    "(for literals like uint MAX = 100) or 'immutable' " +
                    "(for values set once in constructor). Each read saves ~2,100 gas " +
                    "because constant/immutable values are inlined in bytecode.");
        }

        // 2. Storage reads inside loops
        if (code.contains("for") || code.contains("while")) {
            suggestions.add("💡 Cache storage variables before loops. Example: " +
                    "uint len = myArray.length; for(uint i=0; i<len; i++){...} " +
                    "Reading storage costs 2,100 gas each time vs 3 gas for memory. " +
                    "A 100-iteration loop saves 209,700 gas with this fix.");
        }

        // 3. Redundant zero initialization
        if (code.contains("= 0;") || code.contains("= false;") || code.contains("uint i = 0")) {
            suggestions.add("💡 Remove explicit zero initialization. Solidity automatically " +
                    "sets uint to 0, bool to false, address to 0x0. Writing " +
                    "'uint x = 0' wastes gas vs just 'uint x'. " +
                    "This applies to for loop counters too: 'for(uint i;' not 'for(uint i=0;'.");
        }

        // 4. uint8/uint16 packing
        if (code.contains("uint8") || code.contains("uint16") || code.contains("uint32")) {
            suggestions.add("💡 uint8, uint16, uint32 only save gas when packed together " +
                    "in the same storage slot (EVM uses 32-byte slots). " +
                    "If used alone, they actually cost MORE gas because EVM pads them. " +
                    "Pack multiple small uints together in one struct, or use uint256.");
        }

        // 5. String vs bytes32
        if (code.contains("string ")) {
            suggestions.add("💡 If your string is always shorter than 32 characters " +
                    "(names, symbols, short labels), use bytes32 instead of string. " +
                    "bytes32 is a fixed-size value type stored in one slot (cheaper). " +
                    "string is dynamic and requires extra length + data storage.");
        }

        // 6. require() with long strings
        if (code.contains("require(") && code.contains("\"")) {
            suggestions.add("💡 Replace require() error strings with custom errors (Solidity 0.8+). " +
                    "Instead of: require(x > 0, 'Value must be positive'); " +
                    "Use: error ValueMustBePositive(); if(x<=0) revert ValueMustBePositive(); " +
                    "Custom errors save ~50 gas per revert and reduce bytecode size.");
        }

        // 7. Multiple storage writes
        if (code.contains("mapping") && (code.contains("+=") || code.contains("-="))) {
            suggestions.add("💡 Batch storage writes where possible. Instead of updating " +
                    "a mapping 3 times in one function, compute the final value in " +
                    "memory and write once. Each SSTORE (storage write) costs 20,000 gas " +
                    "first time, 2,900 gas for updates.");
        }

        // 8. Events vs storage for off-chain data
        if (code.contains("emit ") && input.getOptimizationCount() > 0) {
            suggestions.add("💡 For data only needed off-chain (audit logs, history), " +
                    "use events instead of storage variables. " +
                    "Emitting an event costs ~375 gas vs 20,000 gas for a storage write. " +
                    "Frontend apps and indexers like The Graph can read events.");
        }

        // 9. Visibility modifiers
        if (code.contains("public ") && !code.contains("external ")) {
            suggestions.add("💡 Mark functions that are only called from outside the contract " +
                    "as 'external' instead of 'public'. External functions read arguments " +
                    "directly from calldata (cheaper) vs public which copies to memory. " +
                    "Savings: ~20-40 gas per call depending on argument size.");
        }

        // 10. Unchecked arithmetic (safe contexts)
        if (code.contains("pragma solidity") && code.contains("0.8")) {
            if (code.contains("for") && code.contains("++")) {
                suggestions.add("💡 In for loops where overflow is impossible, wrap the counter " +
                        "increment in unchecked{}: unchecked{ ++i; } " +
                        "Since Solidity 0.8, every arithmetic op checks for overflow " +
                        "which costs extra gas. A loop counter can never overflow " +
                        "before running out of gas anyway.");
            }
        }

        // 11. Modifier vs internal function
        if (code.contains("modifier ") && code.contains("require(")) {
            suggestions.add("💡 If a modifier's require() check is complex (multiple conditions), " +
                    "move the logic to a private function and call it from the modifier. " +
                    "The Solidity compiler inlines modifier code at every usage point — " +
                    "a function call avoids this duplication in bytecode.");
        }

        // 12. Avoid address(this).balance
        if (code.contains("address(this).balance") || code.contains("this.balance")) {
            suggestions.add("💡 Reading address(this).balance costs gas for an external " +
                    "BALANCE opcode call. If you frequently check your own balance, " +
                    "track it manually with a uint256 internalBalance variable " +
                    "that you update on every deposit/withdrawal.");
        }

        // 13. Delete to get gas refund
        if (code.contains("= 0;") || code.contains("mapping")) {
            suggestions.add("💡 Use 'delete myVariable' instead of setting to zero manually. " +
                    "Deleting storage variables gives a gas refund (up to 4,800 gas in " +
                    "old EVM, reduced in EIP-3529 but still beneficial). " +
                    "delete also works on mappings, arrays, and structs.");
        }

        // 14. Short-circuit evaluation
        if (code.contains("&&") || code.contains("||")) {
            suggestions.add("💡 Order boolean conditions by cheapness in && and || chains. " +
                    "For &&: put the cheapest/most-likely-false condition first " +
                    "(short-circuits immediately on false). " +
                    "For ||: put cheapest/most-likely-true condition first. " +
                    "This avoids evaluating expensive conditions unnecessarily.");
        }

        // 15. No optimization issues at all
        if (suggestions.isEmpty()) {
            suggestions.add("✅ No significant gas inefficiencies found. " +
                    "Your contract follows gas-efficient patterns. " +
                    "For further optimization, consider running the Solidity optimizer " +
                    "with --optimize --optimize-runs=200 during compilation.");
        }

        return suggestions;
    }
}