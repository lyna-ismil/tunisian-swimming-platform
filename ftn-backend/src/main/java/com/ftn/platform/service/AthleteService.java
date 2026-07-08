package com.ftn.platform.service;

import com.ftn.platform.entity.*;
import com.ftn.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final LicenseRepository licenseRepository;
    private final RankingRepository rankingRepository;
    private final NotificationRepository notificationRepository;
    private final CompetitionRegistrationRepository registrationRepository;
    private final CompetitionResultRepository resultRepository;

    public Athlete getAthleteProfile(Long userId) {
        return athleteRepository.findByUserId(userId);
    }

    @Transactional
    public Athlete updateProfile(Long userId, Athlete updatedData) {
        Athlete existing = athleteRepository.findByUserId(userId);
        if (existing == null) {
            updatedData.setUserId(userId);
            return athleteRepository.save(updatedData);
        }
        existing.setFullName(updatedData.getFullName());
        existing.setPhotoUrl(updatedData.getPhotoUrl());
        existing.setDateOfBirth(updatedData.getDateOfBirth());
        existing.setGender(updatedData.getGender());
        existing.setNationality(updatedData.getNationality());
        existing.setClubAffiliation(updatedData.getClubAffiliation());
        existing.setContactPhone(updatedData.getContactPhone());
        return athleteRepository.save(existing);
    }

    public List<License> getLicenses(Long athleteId) {
        return licenseRepository.findByAthleteId(athleteId);
    }

    public List<Ranking> getRankings(Long athleteId) {
        return rankingRepository.findByAthleteIdOrderByUpdateDateDesc(athleteId);
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<CompetitionRegistration> getRegistrations(Long athleteId) {
        return registrationRepository.findByAthleteId(athleteId);
    }

    public List<CompetitionResult> getResults(Long athleteId) {
        return resultRepository.findByAthleteId(athleteId);
    }

    public List<Athlete> getAllAthletes() {
        return athleteRepository.findAll();
    }
}
