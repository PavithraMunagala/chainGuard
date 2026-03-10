package com.pavithra.riskanalyzer.repository;

import com.pavithra.riskanalyzer.entity.ScanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScanRepository extends JpaRepository<ScanEntity, UUID> {
}
