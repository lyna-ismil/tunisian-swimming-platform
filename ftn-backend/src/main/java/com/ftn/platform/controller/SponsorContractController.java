package com.ftn.platform.controller;

import com.ftn.platform.dto.*;
import com.ftn.platform.entity.ContractStatus;
import com.ftn.platform.entity.ContractType;
import com.ftn.platform.service.ContractAIAnalyzerService;
import com.ftn.platform.service.FileStorageService;
import com.ftn.platform.service.SponsorContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/sponsors/contracts")
@RequiredArgsConstructor
public class SponsorContractController {

    private final SponsorContractService contractService;
    private final ContractAIAnalyzerService aiAnalyzerService;
    private final FileStorageService fileStorageService;

    @GetMapping("/sponsor/{sponsorId}")
    public ResponseEntity<List<SponsorContractDTO>> getBySponsor(
            @PathVariable Long sponsorId,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) ContractType type) {
        return ResponseEntity.ok(contractService.getContractsBySponsor(sponsorId, status, type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SponsorContractDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getContractById(id));
    }

    @PostMapping
    public ResponseEntity<SponsorContractDTO> create(@RequestBody ContractCreateRequest request) {
        return ResponseEntity.ok(contractService.createContract(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SponsorContractDTO> update(@PathVariable Long id, @RequestBody ContractCreateRequest request) {
        return ResponseEntity.ok(contractService.updateContract(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/document")
    public ResponseEntity<SponsorContractDTO> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(contractService.uploadDocument(id, file));
    }

    @GetMapping("/{id}/document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        SponsorContractDTO contract = contractService.getContractById(id);
        if (contract.documentUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = fileStorageService.download(contract.documentUrl());
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + contract.contractNumber() + ".pdf\"")
                .body(resource);
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<AIAnalysisResultDTO> analyzeWithAI(@PathVariable Long id) {
        return ResponseEntity.ok(aiAnalyzerService.analyzeContract(id));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SponsorContractDTO> importContractFromPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "sponsorId", required = false) Long sponsorId) {
        SponsorContractDTO dto = contractService.importContractFromPdf(file, sponsorId);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SponsorContractDTO> changeStatus(
            @PathVariable Long id,
            @RequestParam ContractStatus status) {
        return ResponseEntity.ok(contractService.changeStatus(id, status));
    }

    @PostMapping("/{id}/renew")
    public ResponseEntity<SponsorContractDTO> renew(
            @PathVariable Long id,
            @RequestBody RenewalRequest request) {
        return ResponseEntity.ok(contractService.renewContract(id, request));
    }
}
