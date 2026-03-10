package com.pavithra.riskanalyzer.repository;

import com.pavithra.riskanalyzer.entity.SlitherFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SlitherFindingRepository
        extends JpaRepository<SlitherFinding, UUID> {
}
