package com.ftn.platform.service;

import com.ftn.platform.dto.*;
import com.ftn.platform.entity.*;
import com.ftn.platform.exception.EntityNotFoundException;
import com.ftn.platform.repository.SponsorContractRepository;
import com.ftn.platform.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.ftn.platform.service.ContractAIAnalyzerService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SponsorContractService {

    private final SponsorContractRepository contractRepository;
    private final SponsorRepository sponsorRepository;
    private final FileStorageService fileStorageService;
    private final ContractAIAnalyzerService contractAIAnalyzerService;

    public List<SponsorContractDTO> getContractsBySponsor(Long sponsorId, ContractStatus status, ContractType type) {
        List<SponsorContract> contracts;

        if (status != null && type != null) {
            contracts = contractRepository.findBySponsorIdAndStatusAndContractType(sponsorId, status, type);
        } else if (status != null) {
            contracts = contractRepository.findBySponsorIdAndStatus(sponsorId, status);
        } else if (type != null) {
            contracts = contractRepository.findBySponsorIdAndContractType(sponsorId, type);
        } else {
            contracts = contractRepository.findBySponsorId(sponsorId);
        }

        return contracts.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public SponsorContractDTO getContractById(Long id) {
        SponsorContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + id));
        return mapToDTO(contract);
    }

    @Transactional
    public SponsorContractDTO createContract(ContractCreateRequest request) {
        Sponsor sponsor = sponsorRepository.findById(request.sponsorId())
                .orElseThrow(() -> new EntityNotFoundException("Sponsor not found"));

        SponsorContract contract = SponsorContract.builder()
                .sponsor(sponsor)
                .contractNumber(generateContractNumber())
                .contractType(request.contractType())
                .subjectId(request.subjectId())
                .subjectName(request.subjectName())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .value(request.value())
                .description(request.description())
                .status(request.status() != null ? ContractStatus.valueOf(request.status()) : ContractStatus.DRAFT)
                .build();

        SponsorContract saved = contractRepository.save(contract);
        recalculateSponsorStats(sponsor.getId());
        return mapToDTO(saved);
    }

    @Transactional
    public SponsorContractDTO updateContract(Long id, ContractCreateRequest request) {
        SponsorContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));

        contract.setContractType(request.contractType());
        contract.setSubjectId(request.subjectId());
        contract.setSubjectName(request.subjectName());
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setValue(request.value());
        contract.setDescription(request.description());
        if (request.status() != null) {
            contract.setStatus(ContractStatus.valueOf(request.status()));
        }

        SponsorContract saved = contractRepository.save(contract);
        recalculateSponsorStats(contract.getSponsor().getId());
        return mapToDTO(saved);
    }

    @Transactional
    public void deleteContract(Long id) {
        SponsorContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));

        Long sponsorId = contract.getSponsor().getId();

        if (contract.getDocumentUrl() != null) {
            fileStorageService.delete(contract.getDocumentUrl());
        }

        contractRepository.delete(contract);
        recalculateSponsorStats(sponsorId);
    }

    @Transactional
    public SponsorContractDTO uploadDocument(Long id, MultipartFile file) {
        SponsorContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));

        // Delete old document if it exists
        if (contract.getDocumentUrl() != null) {
            fileStorageService.delete(contract.getDocumentUrl());
        }

        String path = fileStorageService.store(file, id);
        contract.setDocumentUrl(path);
        return mapToDTO(contractRepository.save(contract));
    }

    @Transactional
    public SponsorContractDTO importContractFromPdf(MultipartFile file, Long sponsorId) {
        try {
            byte[] bytes = file.getBytes();
            // Extract text
            PDDocument document = Loader.loadPDF(bytes);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();

            // Call AI to extract metadata
            Map<String, Object> metadata = contractAIAnalyzerService.extractContractMetadata(text);

            Sponsor sponsor = null;
            if (sponsorId != null) {
                sponsor = sponsorRepository.findById(sponsorId)
                        .orElseThrow(() -> new EntityNotFoundException("Sponsor not found"));
            } else {
                // Try to detect sponsor from parties
                Object partiesObj = metadata.get("parties");
                List<String> parties = new ArrayList<>();
                if (partiesObj instanceof List) {
                    for (Object o : (List<?>) partiesObj) parties.add(String.valueOf(o));
                }

                List<Sponsor> allSponsors = sponsorRepository.findAll();
                for (String party : parties) {
                    String p = party.toLowerCase();
                    for (Sponsor s : allSponsors) {
                        if (s.getName() == null) continue;
                        String name = s.getName().toLowerCase();
                        if (p.contains(name) || name.contains(p) || p.contains(name.split(" ")[0])) {
                            sponsor = s; break;
                        }
                    }
                    if (sponsor != null) break;
                }

                if (sponsor == null) {
                    throw new EntityNotFoundException("Could not auto-detect sponsor from PDF. Please provide sponsorId manually.");
                }
            }

            // Determine contract type
            ContractType contractType = ContractType.GENERAL;
            if (metadata.containsKey("contractType") && metadata.get("contractType") instanceof String ct) {
                try {
                    contractType = ContractType.valueOf(ct.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown contractType from AI: {}, defaulting to GENERAL", ct);
                }
            }

            // Build contract from metadata
            SponsorContract contract = SponsorContract.builder()
                    .sponsor(sponsor)
                    .contractNumber(generateContractNumber())
                    .contractType(contractType)
                    .status(ContractStatus.DRAFT)
                    .build();

            // Populate fields if available
            if (metadata.containsKey("financialValue") && metadata.get("financialValue") instanceof Map) {
                Map<?,?> fv = (Map<?,?>) metadata.get("financialValue");
                Object amt = fv.get("amount");
                if (amt instanceof Number) contract.setValue(java.math.BigDecimal.valueOf(((Number) amt).doubleValue()));
            }
            if (metadata.containsKey("contractSummary")) {
                contract.setAiSummary(String.valueOf(metadata.get("contractSummary")));
            }
            if (metadata.containsKey("duration") && metadata.get("duration") instanceof Map) {
                Map<?,?> dur = (Map<?,?>) metadata.get("duration");
                try {
                    if (dur.get("start") != null) contract.setStartDate(LocalDate.parse(String.valueOf(dur.get("start"))));
                    if (dur.get("end") != null) contract.setEndDate(LocalDate.parse(String.valueOf(dur.get("end"))));
                } catch (Exception ex) {
                    // ignore parse errors
                }
            }

            SponsorContract saved = contractRepository.save(contract);

            // Store PDF
            String path = fileStorageService.store(file, saved.getId());
            saved.setDocumentUrl(path);
            saved = contractRepository.save(saved);

            // Recalculate sponsor stats
            recalculateSponsorStats(sponsor.getId());

            return mapToDTO(saved);
        } catch (EntityNotFoundException enf) {
            throw enf;
        } catch (Exception e) {
            log.error("Failed to import contract from PDF", e);
            throw new RuntimeException("Failed to import contract from PDF: " + e.getMessage(), e);
        }
    }

    @Transactional
    public SponsorContractDTO changeStatus(Long id, ContractStatus newStatus) {
        SponsorContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));

        contract.setStatus(newStatus);
        SponsorContract saved = contractRepository.save(contract);
        recalculateSponsorStats(contract.getSponsor().getId());
        return mapToDTO(saved);
    }

    @Transactional
    public SponsorContractDTO renewContract(Long id, RenewalRequest request) {
        SponsorContract oldContract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));

        // Mark old as RENEWED
        oldContract.setStatus(ContractStatus.RENEWED);
        contractRepository.save(oldContract);

        // Create new contract
        SponsorContract newContract = SponsorContract.builder()
                .sponsor(oldContract.getSponsor())
                .contractNumber(generateContractNumber())
                .contractType(oldContract.getContractType())
                .subjectId(oldContract.getSubjectId())
                .subjectName(oldContract.getSubjectName())
                .startDate(oldContract.getEndDate() != null ? oldContract.getEndDate() : LocalDate.now())
                .endDate(request.newEndDate())
                .value(request.newValue() != null ? request.newValue() : oldContract.getValue())
                .description(oldContract.getDescription())
                .status(ContractStatus.ACTIVE)
                .build();

        SponsorContract saved = contractRepository.save(newContract);
        recalculateSponsorStats(oldContract.getSponsor().getId());
        return mapToDTO(saved);
    }

    @Transactional
    public void recalculateSponsorStats(Long sponsorId) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId).orElse(null);
        if (sponsor == null) return;

        BigDecimal totalValue = contractRepository.sumActiveContractValues(sponsorId);
        long athleteCount = contractRepository.countActiveContractsByType(sponsorId, ContractType.ATHLETE);
        long competitionCount = contractRepository.countActiveContractsByType(sponsorId, ContractType.COMPETITION);

        if (totalValue != null && totalValue.compareTo(BigDecimal.ZERO) > 0) {
            sponsor.setTotalValue(totalValue.doubleValue());
        } else if (sponsor.getTotalValue() == null) {
            sponsor.setTotalValue(0.0);
        }
        
        sponsor.setAthletesCount((int) athleteCount);
        sponsor.setCompetitionsCount((int) competitionCount);
        sponsorRepository.save(sponsor);
    }

    private String generateContractNumber() {
        String year = String.valueOf(Year.now().getValue());
        String prefix = "CONT-" + year + "-";
        long count = contractRepository.countByContractNumberStartingWith(prefix);
        return String.format("%s%04d", prefix, count + 1);
    }

    private SponsorContractDTO mapToDTO(SponsorContract c) {
        List<ContractClauseDTO> clauseDTOs = c.getClauses() != null
                ? c.getClauses().stream().map(cl -> new ContractClauseDTO(
                    cl.getId(), cl.getClauseType(), cl.getExtractedText(),
                    cl.getAiInterpretation(), cl.getPageNumber(), cl.getConfidenceScore()
                )).collect(Collectors.toList())
                : List.of();

        return new SponsorContractDTO(
                c.getId(),
                c.getSponsor().getId(),
                c.getSponsor().getName(),
                c.getContractNumber(),
                c.getContractType(),
                c.getSubjectId(),
                c.getSubjectName(),
                c.getStartDate(),
                c.getEndDate(),
                c.getValue(),
                c.getCurrency(),
                c.getStatus(),
                c.getDescription(),
                c.getDocumentUrl(),
                c.getAiSummary(),
                clauseDTOs,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
