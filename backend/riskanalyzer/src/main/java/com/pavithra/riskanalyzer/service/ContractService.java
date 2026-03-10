package com.pavithra.riskanalyzer.service;

import com.pavithra.riskanalyzer.dto.VerifiedContractDTO;
import com.pavithra.riskanalyzer.entity.ContractEntity;
import com.pavithra.riskanalyzer.repository.ContractRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final EtherscanService etherscanService;

    public ContractService(
            ContractRepository contractRepository,
            EtherscanService etherscanService
    ) {
        this.contractRepository = contractRepository;
        this.etherscanService = etherscanService;
    }

    /** Step 1: create scan */
    public ContractEntity createScan(String address) {

        ContractEntity contract = new ContractEntity();
        contract.setContractId(UUID.randomUUID());
        contract.setContractAddress(address);
        contract.setInputType("ADDRESS");
        contract.setNetwork("mainnet");

        contract.setScanStatus("CREATED");
        contract.setSourceAvailable(false);
        contract.setAnalysisMode("UNKNOWN");
        contract.setMessage("Scan created");

        return contractRepository.save(contract);
    }

    /** Step 2: fetch + persist (SOURCE or BYTECODE) */
    public ContractEntity fetchAndPersist(ContractEntity contract) {

        try {
            VerifiedContractDTO dto =
                    etherscanService.fetchVerifiedContract(contract.getContractAddress());

            contract.setSourceAvailable(true);
            contract.setAnalysisMode("SOURCE");
            contract.setScanStatus("FETCHED");
            contract.setMessage("Verified source code + metadata fetched");

            contract.setSourceCode(dto.sourceCode());
            contract.setAbi(dto.abi());
            contract.setContractName(dto.contractName());
            contract.setCompilerVersion(dto.compilerVersion());

        } catch (Exception e) {

            contract.setSourceAvailable(false);
            contract.setAnalysisMode("BYTECODE");
            contract.setScanStatus("FETCHED");
            contract.setMessage(e.getMessage());
        }

        return contractRepository.save(contract);
    }


    /** Read-only access */
    public ContractEntity getScan(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Scan not found"));
    }
}
