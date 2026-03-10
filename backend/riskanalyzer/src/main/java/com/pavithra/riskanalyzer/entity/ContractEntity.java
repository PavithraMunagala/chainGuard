package com.pavithra.riskanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contracts")
public class ContractEntity {

    @Id
    private UUID contractId;

    // ---------------- Basic Info ----------------
    private String contractAddress;   // Ethereum address
    private String inputType;          // ADDRESS | FILE | RAW
    private String network;            // mainnet / testnet

    // ---------------- Source Metadata ----------------
    private String contractName;
    private String compilerVersion;

    @Column(columnDefinition = "TEXT")
    private String abi;

    @Column(columnDefinition = "TEXT")
    private String sourceCode;

    @Column(columnDefinition = "TEXT")
    private String bytecode;

    // ---------------- Scan Metadata ----------------
    private boolean sourceAvailable;   // true if verified source exists
    private String analysisMode;       // SOURCE | BYTECODE
    private String scanStatus;         // CREATED | FETCHED | FAILED
    private String message;            // info / error message

    private Instant createdAt = Instant.now();

    // ---------------- Getters & Setters ----------------

    public UUID getContractId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getCompilerVersion() {
        return compilerVersion;
    }

    public void setCompilerVersion(String compilerVersion) {
        this.compilerVersion = compilerVersion;
    }

    public String getAbi() {
        return abi;
    }

    public void setAbi(String abi) {
        this.abi = abi;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getBytecode() {
        return bytecode;
    }

    public void setBytecode(String bytecode) {
        this.bytecode = bytecode;
    }

    public boolean isSourceAvailable() {
        return sourceAvailable;
    }

    public void setSourceAvailable(boolean sourceAvailable) {
        this.sourceAvailable = sourceAvailable;
    }

    public String getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(String analysisMode) {
        this.analysisMode = analysisMode;
    }

    public String getScanStatus() {
        return scanStatus;
    }

    public void setScanStatus(String scanStatus) {
        this.scanStatus = scanStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
