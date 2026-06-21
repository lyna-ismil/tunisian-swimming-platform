package com.ftn.platform.service;

import com.ftn.platform.dto.CompetitionEventDTO;
import com.ftn.platform.dto.SponsorshipPackageDTO;
import com.ftn.platform.entity.CompetitionEvent;
import com.ftn.platform.entity.PackageStatus;
import com.ftn.platform.entity.SponsorshipPackage;
import com.ftn.platform.repository.CompetitionEventRepository;
import com.ftn.platform.repository.SponsorshipPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompetitionEventService {

    private final CompetitionEventRepository eventRepository;
    private final SponsorshipPackageRepository packageRepository;

    public List<CompetitionEventDTO> getAllEvents() {
        return eventRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public CompetitionEventDTO getEventById(Long id) {
        CompetitionEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Event not found: " + id));
        return mapToDTO(event);
    }

    @Transactional
    public CompetitionEventDTO createEvent(CompetitionEventDTO dto) {
        CompetitionEvent event = CompetitionEvent.builder()
                .name(dto.name())
                .bannerUrl(dto.bannerUrl())
                .eventDate(dto.eventDate())
                .location(dto.location())
                .athletesCount(dto.athletesCount())
                .clubsCount(dto.clubsCount())
                .audience(dto.audience())
                .streaming(dto.streaming())
                .revenueTarget(dto.revenueTarget())
                .build();
        return mapToDTO(eventRepository.save(event));
    }

    @Transactional
    public CompetitionEventDTO updateEvent(Long id, CompetitionEventDTO dto) {
        CompetitionEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Event not found: " + id));
        
        event.setName(dto.name());
        event.setBannerUrl(dto.bannerUrl());
        event.setEventDate(dto.eventDate());
        event.setLocation(dto.location());
        event.setAthletesCount(dto.athletesCount());
        event.setClubsCount(dto.clubsCount());
        event.setAudience(dto.audience());
        event.setStreaming(dto.streaming());
        event.setRevenueTarget(dto.revenueTarget());
        
        return mapToDTO(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    @Transactional
    public SponsorshipPackageDTO addPackage(Long eventId, SponsorshipPackageDTO dto) {
        CompetitionEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Event not found: " + eventId));
        
        SponsorshipPackage pkg = SponsorshipPackage.builder()
                .competitionEvent(event)
                .title(dto.title())
                .price(dto.price())
                .benefits(dto.benefits())
                .status(PackageStatus.valueOf(dto.status().toUpperCase()))
                .sponsorName(dto.sponsorName())
                .build();
        
        return mapPackageToDTO(packageRepository.save(pkg));
    }

    @Transactional
    public SponsorshipPackageDTO updatePackage(Long eventId, Long packageId, SponsorshipPackageDTO dto) {
        SponsorshipPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Package not found: " + packageId));
        
        if (!pkg.getCompetitionEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Package does not belong to the specified event");
        }
        
        pkg.setTitle(dto.title());
        pkg.setPrice(dto.price());
        pkg.setBenefits(dto.benefits());
        pkg.setStatus(PackageStatus.valueOf(dto.status().toUpperCase()));
        pkg.setSponsorName(dto.sponsorName());
        
        return mapPackageToDTO(packageRepository.save(pkg));
    }

    @Transactional
    public void deletePackage(Long eventId, Long packageId) {
        SponsorshipPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Package not found: " + packageId));
        
        if (!pkg.getCompetitionEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Package does not belong to the specified event");
        }
        
        packageRepository.deleteById(packageId);
    }

    private CompetitionEventDTO mapToDTO(CompetitionEvent e) {
        List<SponsorshipPackageDTO> packages = e.getPackages().stream()
                .map(this::mapPackageToDTO)
                .collect(Collectors.toList());
        
        return new CompetitionEventDTO(
                e.getId(), e.getName(), e.getBannerUrl(), e.getEventDate(),
                e.getLocation(), e.getAthletesCount(), e.getClubsCount(),
                e.getAudience(), e.getStreaming(), e.getRevenueTarget(), packages
        );
    }

    private SponsorshipPackageDTO mapPackageToDTO(SponsorshipPackage p) {
        return new SponsorshipPackageDTO(
                p.getId(), p.getTitle(), p.getPrice(), p.getBenefits(),
                p.getStatus().name(), p.getSponsorName()
        );
    }
}
