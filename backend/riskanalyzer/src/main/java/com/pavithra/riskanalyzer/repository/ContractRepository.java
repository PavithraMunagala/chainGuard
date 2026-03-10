package com.pavithra.riskanalyzer.repository;

import com.pavithra.riskanalyzer.entity.ContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<ContractEntity, UUID> {
    Optional<ContractEntity> findByContractAddress(String contractAddress);
}
