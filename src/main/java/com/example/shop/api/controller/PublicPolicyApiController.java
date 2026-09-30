package com.example.shop.api.controller;

import java.util.List;

import com.example.shop.api.dto.policy.PolicyResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.domain.Policy;
import com.example.shop.service.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
public class PublicPolicyApiController {
    private final PolicyService policyService;

    public PublicPolicyApiController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public List<PolicyResponse> listActive() {
        return policyService.getActivePolicies().stream().filter(Policy::isTechnologyCatalog).map(PolicyResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponse> getById(@PathVariable Long id) {
        Policy policy = policyService.getPolicyById(id);
        if (policy == null || !policy.isActive() || !policy.isTechnologyCatalog()) {
            throw new ResourceNotFoundException("Chính sách", id);
        }
        return ResponseEntity.ok(PolicyResponse.from(policy));
    }
}
