package com.uniandes.admin_api.web.controller;

import com.uniandes.admin_api.domain.model.VendorCommissionDTO;
import com.uniandes.admin_api.domain.service.CommissionService;
import com.uniandes.admin_api.web.dto.VendorCommissionRequest;
import com.uniandes.admin_api.web.dto.VendorCommissionResponse;
import com.uniandes.admin_api.web.mapper.VendorCommissionWebMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/vendor")
public class VendorController {

    private final CommissionService commissionService;
    private final VendorCommissionWebMapper mapper;

    public VendorController(CommissionService commissionService, VendorCommissionWebMapper mapper) {
        this.commissionService = commissionService;
        this.mapper = mapper;
    }

    @GetMapping("ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.status(HttpStatus.OK)
                .body("pong");
    }

    @GetMapping("commission")
    public ResponseEntity<List<VendorCommissionResponse>> getAllCommissions() {
        List<VendorCommissionResponse> response = commissionService.findAll().stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("commission/{id}")
    public ResponseEntity<VendorCommissionResponse> getCommission(@PathVariable Long id) {
        VendorCommissionDTO commission = commissionService.findById(id);
        VendorCommissionResponse response = mapper.toResponse(commission);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("commission")
    public ResponseEntity<VendorCommissionResponse> createCommission(@Valid @RequestBody VendorCommissionRequest request) {
        VendorCommissionDTO dto = mapper.fromRequest(request);
        VendorCommissionDTO created = commissionService.create(dto);
        VendorCommissionResponse response = mapper.toResponse(created);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("commission/{id}")
    public ResponseEntity<VendorCommissionResponse> updateCommission(
            @PathVariable Long id,
            @Valid @RequestBody VendorCommissionRequest request) {
        VendorCommissionDTO dto = mapper.fromRequest(request);
        VendorCommissionDTO updated = commissionService.update(id, dto);
        VendorCommissionResponse response = mapper.toResponse(updated);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping("commission/{id}")
    public ResponseEntity<Void> deleteCommission(@PathVariable Long id) {
        commissionService.delete(id);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }
}
