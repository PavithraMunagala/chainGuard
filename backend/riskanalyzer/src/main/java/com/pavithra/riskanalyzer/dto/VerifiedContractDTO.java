package com.pavithra.riskanalyzer.dto;

/**
 * Immutable DTO representing a verified contract response from Etherscan.
 * This is the ONLY DTO ContractService should use.
 */
public record VerifiedContractDTO(
        String sourceCode,
        String abi,
        String contractName,
        String compilerVersion,
        boolean isProxy,
        String implementation
) {}
