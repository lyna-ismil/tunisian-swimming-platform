package com.ftn.platform.controller;

import com.ftn.platform.dto.SponsorshipRequestDTO;
import com.ftn.platform.service.SponsorshipRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sponsors/requests")
@RequiredArgsConstructor
public class SponsorshipRequestController {

    private final SponsorshipRequestService requestService;

    @GetMapping
    public List<SponsorshipRequestDTO> getRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search) {
        return requestService.getRequests(status, type, priority, search);
    }

    @GetMapping("/{id}")
    public SponsorshipRequestDTO getRequestById(@PathVariable String id) {
        return requestService.getRequestById(id);
    }

    @PostMapping
    public SponsorshipRequestDTO createRequest(@RequestBody SponsorshipRequestDTO requestDTO) {
        return requestService.createRequest(requestDTO);
    }

    @PutMapping("/{id}")
    public SponsorshipRequestDTO updateRequest(@PathVariable String id, @RequestBody SponsorshipRequestDTO requestDTO) {
        return requestService.updateRequest(id, requestDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteRequest(@PathVariable String id) {
        requestService.deleteRequest(id);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return requestService.getStats();
    }
}
