package com.pavithra.riskanalyzer.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "slither_findings")
public class SlitherFinding {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID contractId;

    @Column(columnDefinition = "TEXT")
    private String rawJson;

    @Column(columnDefinition = "TEXT")
    private String advisoryJson;

    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public UUID getContractId() {
        return contractId;
    }


    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAdvisoryJson() {
        return advisoryJson;
    }

    public void setAdvisoryJson(String advisoryJson) {
        this.advisoryJson = advisoryJson;
    }
}
