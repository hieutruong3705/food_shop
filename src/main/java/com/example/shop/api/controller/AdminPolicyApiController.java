package com.example.shop.api.controller;

import java.util.List;

import com.example.shop.api.dto.policy.PolicyRequest;
import com.example.shop.api.dto.policy.PolicyResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.domain.Policy;
import com.example.shop.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/admin/policies")
public class AdminPolicyApiController {
    private final PolicyService policyService;

    public AdminPolicyApiController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public List<PolicyResponse> list(@org.springframework.web.bind.annotation.RequestParam(defaultValue="") String search) {
        if(search.length()>100)throw new IllegalArgumentException("Từ khóa quá dài");
        return policyService.getAllPolicies().stream().filter(p->p.getName().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))).map(PolicyResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PolicyResponse getById(@PathVariable Long id) {
        return PolicyResponse.from(requirePolicy(id));
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody PolicyRequest request) {
        Policy policy = new Policy();
        policy.setActive(request.active() == null || request.active());
        apply(request, policy);
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyResponse.from(policyService.savePolicy(policy)));
    }

    @PutMapping("/{id}")
    public PolicyResponse update(@PathVariable Long id, @Valid @RequestBody PolicyRequest request) {
        Policy policy = requirePolicy(id);
        apply(request, policy);
        return PolicyResponse.from(policyService.savePolicy(policy));
    }

    @PatchMapping("/{id}/active")
    public PolicyResponse changeActive(@PathVariable Long id, @RequestBody PolicyRequest request) {
        if (request.active() == null) {
            throw new IllegalArgumentException("Trạng thái active là bắt buộc");
        }
        Policy policy = requirePolicy(id);
        policy.setActive(request.active());
        return PolicyResponse.from(policyService.savePolicy(policy));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        policyService.deletePolicy(requirePolicy(id).getId());
        return ResponseEntity.noContent().build();
    }

    private Policy requirePolicy(Long id) {
        Policy policy = policyService.getPolicyById(id);
        if (policy == null) {
            throw new ResourceNotFoundException("Chính sách", id);
        }
        return policy;
    }

    private void apply(PolicyRequest request, Policy policy) {
        if(request.technologyCatalog()!=null)policy.setTechnologyCatalog(request.technologyCatalog());
        policy.setName(request.name().trim());
        policy.setContent(request.content());
        if (request.active() != null) {
            policy.setActive(request.active());
        }
    }
}
