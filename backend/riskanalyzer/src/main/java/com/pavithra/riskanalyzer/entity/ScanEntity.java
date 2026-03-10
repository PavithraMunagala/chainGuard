package com.pavithra.riskanalyzer.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scans")
public class ScanEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private ContractEntity contract;

    private String analysisMode; // SOURCE | BYTECODE_ONLY

    private Instant createdAt = Instant.now();

    // getters & setters
}
