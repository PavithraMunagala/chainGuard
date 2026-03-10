package com.pavithra.riskanalyzer.service;

import com.pavithra.riskanalyzer.dto.VerifiedContractDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EtherscanService {

    @Value("${etherscan.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public VerifiedContractDTO fetchVerifiedContract(String address) {

        String url = "https://api.etherscan.io/v2/api" +
                "?chainid=1" +
                "&module=contract" +
                "&action=getsourcecode" +
                "&address=" + address +
                "&apikey=" + apiKey;

        ResponseEntity<Map> response =
                restTemplate.getForEntity(url, Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null || !"1".equals(String.valueOf(body.get("status")))) {
            throw new RuntimeException("Etherscan error: " + body);
        }

        List<Map<String, Object>> result =
                (List<Map<String, Object>>) body.get("result");

        Map<String, Object> data = result.get(0);

        String sourceCode = String.valueOf(data.get("SourceCode")).trim();
        if (sourceCode.isEmpty() || sourceCode.equals("[]")) {
            throw new RuntimeException("Contract not verified");
        }

        return new VerifiedContractDTO(
                sourceCode,
                String.valueOf(data.get("ABI")),
                String.valueOf(data.get("ContractName")),
                String.valueOf(data.get("CompilerVersion")),
                "1".equals(String.valueOf(data.get("Proxy"))),
                String.valueOf(data.get("Implementation"))
        );
    }

}
