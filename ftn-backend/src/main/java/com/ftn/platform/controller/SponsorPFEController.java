package com.ftn.platform.controller;

import com.ftn.platform.dto.SponsorPFEDTO;
import com.ftn.platform.entity.PFEStatus;
import com.ftn.platform.service.SponsorPFEService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sponsors/pfe")
@RequiredArgsConstructor
public class SponsorPFEController {

    private final SponsorPFEService pfeService;

    @GetMapping("/sponsor/{sponsorId}")
    public ResponseEntity<List<SponsorPFEDTO>> getBySponsor(@PathVariable Long sponsorId) {
        return ResponseEntity.ok(pfeService.getBySponsor(sponsorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SponsorPFEDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pfeService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SponsorPFEDTO> create(
            @RequestBody SponsorPFEDTO dto,
            @RequestParam(defaultValue = "false") boolean createLinkedContract) {
        return ResponseEntity.ok(pfeService.createPFE(dto, createLinkedContract));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SponsorPFEDTO> update(@PathVariable Long id, @RequestBody SponsorPFEDTO dto) {
        return ResponseEntity.ok(pfeService.updatePFE(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pfeService.deletePFE(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SponsorPFEDTO> changeStatus(
            @PathVariable Long id,
            @RequestParam PFEStatus status) {
        return ResponseEntity.ok(pfeService.changeStatus(id, status));
    }

    @PostMapping("/{pfeId}/link/{contractId}")
    public ResponseEntity<SponsorPFEDTO> linkToContract(
            @PathVariable Long pfeId,
            @PathVariable Long contractId) {
        return ResponseEntity.ok(pfeService.linkToContract(pfeId, contractId));
    }
}
