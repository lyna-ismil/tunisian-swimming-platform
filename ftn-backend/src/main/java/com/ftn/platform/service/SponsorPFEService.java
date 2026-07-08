package com.ftn.platform.service;

import com.ftn.platform.dto.SponsorPFEDTO;
import com.ftn.platform.entity.*;
import com.ftn.platform.exception.EntityNotFoundException;
import com.ftn.platform.repository.SponsorContractRepository;
import com.ftn.platform.repository.SponsorPFERepository;
import com.ftn.platform.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SponsorPFEService {

    private final SponsorPFERepository pfeRepository;
    private final SponsorRepository sponsorRepository;
    private final SponsorContractRepository contractRepository;

    public List<SponsorPFEDTO> getBySponsor(Long sponsorId) {
        return pfeRepository.findBySponsorId(sponsorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public SponsorPFEDTO getById(Long id) {
        return mapToDTO(pfeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PFE not found")));
    }

    @Transactional
    public SponsorPFEDTO createPFE(SponsorPFEDTO dto, boolean createLinkedContract) {
        Sponsor sponsor = sponsorRepository.findById(dto.sponsorId())
                .orElseThrow(() -> new EntityNotFoundException("Sponsor not found"));

        SponsorPFE pfe = SponsorPFE.builder()
                .sponsor(sponsor)
                .studentName(dto.studentName())
                .studentEmail(dto.studentEmail())
                .university(dto.university())
                .specialty(dto.specialty())
                .pfeTitle(dto.pfeTitle())
                .pfeDescription(dto.pfeDescription())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .status(dto.status() != null ? dto.status() : PFEStatus.PROPOSED)
                .sponsorGrant(dto.sponsorGrant())
                .build();

        if (createLinkedContract) {
            SponsorContract contract = SponsorContract.builder()
                    .sponsor(sponsor)
                    .contractNumber(generateContractNumber())
                    .contractType(ContractType.PFE)
                    .subjectName(dto.studentName() + " — " + dto.pfeTitle())
                    .startDate(dto.startDate())
                    .endDate(dto.endDate())
                    .value(dto.sponsorGrant())
                    .description("PFE sponsorship: " + dto.pfeTitle())
                    .status(ContractStatus.DRAFT)
                    .build();
            contract = contractRepository.save(contract);
            pfe.setContract(contract);
        }

        return mapToDTO(pfeRepository.save(pfe));
    }

    @Transactional
    public SponsorPFEDTO updatePFE(Long id, SponsorPFEDTO dto) {
        SponsorPFE pfe = pfeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PFE not found"));

        pfe.setStudentName(dto.studentName());
        pfe.setStudentEmail(dto.studentEmail());
        pfe.setUniversity(dto.university());
        pfe.setSpecialty(dto.specialty());
        pfe.setPfeTitle(dto.pfeTitle());
        pfe.setPfeDescription(dto.pfeDescription());
        pfe.setStartDate(dto.startDate());
        pfe.setEndDate(dto.endDate());
        if (dto.status() != null) pfe.setStatus(dto.status());
        pfe.setSponsorGrant(dto.sponsorGrant());

        return mapToDTO(pfeRepository.save(pfe));
    }

    @Transactional
    public void deletePFE(Long id) {
        pfeRepository.deleteById(id);
    }

    @Transactional
    public SponsorPFEDTO changeStatus(Long id, PFEStatus status) {
        SponsorPFE pfe = pfeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PFE not found"));
        pfe.setStatus(status);
        return mapToDTO(pfeRepository.save(pfe));
    }

    @Transactional
    public SponsorPFEDTO linkToContract(Long pfeId, Long contractId) {
        SponsorPFE pfe = pfeRepository.findById(pfeId)
                .orElseThrow(() -> new EntityNotFoundException("PFE not found"));
        SponsorContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        pfe.setContract(contract);
        return mapToDTO(pfeRepository.save(pfe));
    }

    private String generateContractNumber() {
        String year = String.valueOf(Year.now().getValue());
        String prefix = "CONT-" + year + "-";
        long count = contractRepository.countByContractNumberStartingWith(prefix);
        return String.format("%s%04d", prefix, count + 1);
    }

    private SponsorPFEDTO mapToDTO(SponsorPFE p) {
        return new SponsorPFEDTO(
                p.getId(),
                p.getSponsor().getId(),
                p.getContract() != null ? p.getContract().getId() : null,
                p.getStudentName(),
                p.getStudentEmail(),
                p.getUniversity(),
                p.getSpecialty(),
                p.getPfeTitle(),
                p.getPfeDescription(),
                p.getStartDate(),
                p.getEndDate(),
                p.getStatus(),
                p.getSponsorGrant(),
                p.getCreatedAt()
        );
    }
}
