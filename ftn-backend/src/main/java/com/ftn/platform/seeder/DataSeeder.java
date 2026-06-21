package com.ftn.platform.seeder;

import com.ftn.platform.entity.*;
import com.ftn.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.ftn.platform.service.SmartMatchingService;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final SponsorRepository sponsorRepository;
        private final SponsorPreferencesRepository sponsorPreferencesRepository;
        private final AthleteRepository athleteRepository;
        private final RankingRepository rankingRepository;
        private final LicenseRepository licenseRepository;
        private final CompetitionResultRepository resultRepository;
    private final SponsorshipRequestRepository requestRepository;
    private final AthleteMatchRepository matchRepository;
    private final CompetitionEventRepository eventRepository;
        private final SmartMatchingService smartMatchingService;
    private final SuggestionRepository suggestionRepository;
    private final SystemPromptRepository systemPromptRepository;
    private final ChatConfigRepository chatConfigRepository;

    @Override
    public void run(String... args) throws Exception {
        if (sponsorRepository.count() == 0) {
            log.info("Seeding Sponsors...");
            seedSponsors();
        }

                if (athleteRepository.count() == 0) {
                        log.info("Seeding Athletes...");
                        seedAthletes();
                }
        
        if (requestRepository.count() == 0) {
            log.info("Seeding Sponsorship Requests...");
            seedRequests();
        }
        
        if (matchRepository.count() == 0) {
            log.info("Seeding Athlete Matches...");
            seedMatches();
        }
        
        if (eventRepository.count() == 0) {
            log.info("Seeding Competition Events...");
            seedEvents();
        }

                seedSponsorPreferences();

        if (suggestionRepository.count() == 0) {
            log.info("Seeding Suggestions...");
            seedSuggestions();
        }

        if (systemPromptRepository.count() == 0) {
            log.info("Seeding System Prompt...");
            systemPromptRepository.save(SystemPrompt.builder()
                .promptText(com.ftn.platform.service.GroqChatService.DEFAULT_SYSTEM_PROMPT)
                .version(1)
                .isActive(true)
                .build());
        }

        if (!chatConfigRepository.existsById("welcome_message")) {
            chatConfigRepository.save(ChatConfig.builder()
                .configKey("welcome_message")
                .configValue("Hello! I am the FTN AI Assistant. I can help you with competition rules, athlete profiles, administrative procedures, or navigating the platform. How can I assist you today?")
                .build());
        }

        log.info("Seeding completed!");
    }

    private void seedSponsors() {
        Sponsor s1 = Sponsor.builder()
                .name("Speedo")
                .tier("PLATINUM")
                .description("Official swimwear and equipment partner for the national team.")
                .logoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Speedo_logo.svg/512px-Speedo_logo.svg.png")
                .website("https://speedo.com")
                .contactEmail("sponsorships@speedo.com")
                .status(SponsorStatus.ACTIVE)
                .athletesCount(142)
                .competitionsCount(12)
                .startDate(LocalDate.parse("2022-01-15"))
                .totalValue(250000.0)
                .preferredDisciplines("[\"Freestyle\", \"Medley\", \"Sprint Freestyle\"]")
                .build();

        Sponsor s2 = Sponsor.builder()
                .name("Ooredoo Tunisia")
                .tier("GOLD")
                .description("Telecommunications partner supporting local athletic development.")
                .logoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Ooredoo_logo.svg/512px-Ooredoo_logo.svg.png")
                .website("https://ooredoo.tn")
                .contactEmail("sports@ooredoo.tn")
                .status(SponsorStatus.ACTIVE)
                .athletesCount(85)
                .competitionsCount(8)
                .startDate(LocalDate.parse("2023-03-01"))
                .totalValue(120000.0)
                .preferredDisciplines("[\"Butterfly\", \"Freestyle\"]")
                .build();

        Sponsor s3 = Sponsor.builder()
                .name("Arena")
                .tier("SILVER")
                .description("Supporting junior swimming clubs with training gear.")
                .logoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/6/6d/Arena_logo.svg/512px-Arena_logo.svg.png")
                .website("https://arenawaterinstinct.com")
                .contactEmail("partnerships@arena.com")
                .status(SponsorStatus.EXPIRING_SOON)
                .athletesCount(45)
                .competitionsCount(4)
                .startDate(LocalDate.parse("2021-06-10"))
                .totalValue(50000.0)
                .preferredDisciplines("[\"Junior Development\", \"Butterfly\", \"Backstroke\"]")
                .build();

        Sponsor s4 = Sponsor.builder()
                .name("Tunisair")
                .tier("GOLD")
                .description("Official travel partner for international competitions.")
                .logoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Tunisair_logo.svg/512px-Tunisair_logo.svg.png")
                .website("https://tunisair.com")
                .contactEmail("sponsorship@tunisair.com")
                .status(SponsorStatus.ACTIVE)
                .athletesCount(0)
                .competitionsCount(15)
                .startDate(LocalDate.parse("2024-01-01"))
                .totalValue(180000.0)
                                .preferredDisciplines("[\"Travel Team\", \"Open Water\"]")
                .build();

        sponsorRepository.saveAll(List.of(s1, s2, s3, s4));
    }

        private void seedAthletes() {
                Athlete a1 = Athlete.builder().fullName("Ahmed Hafnaoui").photoUrl("https://ui-avatars.com/api/?name=Ahmed+Hafnaoui&background=0EA5E9&color=fff").dateOfBirth(LocalDate.parse("2002-09-02")).gender("Male").nationality("Tunisian").clubAffiliation("Club Africain").contactPhone("+21620000001").build();
                Athlete a2 = Athlete.builder().fullName("Sarra Lajnef").photoUrl("https://ui-avatars.com/api/?name=Sarra+Lajnef&background=2563EB&color=fff").dateOfBirth(LocalDate.parse("1996-04-12")).gender("Female").nationality("Tunisian").clubAffiliation("CSS").contactPhone("+21620000002").build();
                Athlete a3 = Athlete.builder().fullName("Rami Chammari").photoUrl("https://ui-avatars.com/api/?name=Rami+Chammari&background=7C3AED&color=fff").dateOfBirth(LocalDate.parse("2007-01-18")).gender("Male").nationality("Tunisian").clubAffiliation("Espérance Sportive de Tunis").contactPhone("+21620000003").build();
                Athlete a4 = Athlete.builder().fullName("Nesrine Medjahed").photoUrl("https://ui-avatars.com/api/?name=Nesrine+Medjahed&background=059669&color=fff").dateOfBirth(LocalDate.parse("2004-11-05")).gender("Female").nationality("Tunisian").clubAffiliation("Club Africain").contactPhone("+21620000004").build();
                Athlete a5  = Athlete.builder().fullName("Mohamed Khalil").photoUrl("https://ui-avatars.com/api/?name=Mohamed+Khalil&background=F97316&color=fff").dateOfBirth(LocalDate.parse("2001-06-21")).gender("Male").nationality("Tunisian").clubAffiliation("Stade Tunisien").contactPhone("+21620000005").build();
                Athlete a6  = Athlete.builder().fullName("Youssef El-Kilani").photoUrl("https://ui-avatars.com/api/?name=Youssef+El-Kilani&background=DC2626&color=fff").dateOfBirth(LocalDate.parse("2005-03-15")).gender("Male").nationality("Tunisian").clubAffiliation("US Monastir").contactPhone("+21620000006").build();
                Athlete a7  = Athlete.builder().fullName("Amira Ben Ammar").photoUrl("https://ui-avatars.com/api/?name=Amira+Ben+Ammar&background=DB2777&color=fff").dateOfBirth(LocalDate.parse("2003-07-22")).gender("Female").nationality("Tunisian").clubAffiliation("CSS").contactPhone("+21620000007").build();
                Athlete a8  = Athlete.builder().fullName("Omar Jebali").photoUrl("https://ui-avatars.com/api/?name=Omar+Jebali&background=65A30D&color=fff").dateOfBirth(LocalDate.parse("2000-11-30")).gender("Male").nationality("Tunisian").clubAffiliation("Stade Tunisien").contactPhone("+21620000008").build();
                Athlete a9  = Athlete.builder().fullName("Leila Trabelsi").photoUrl("https://ui-avatars.com/api/?name=Leila+Trabelsi&background=0891B2&color=fff").dateOfBirth(LocalDate.parse("2006-05-08")).gender("Female").nationality("Tunisian").clubAffiliation("Espérance Sportive de Tunis").contactPhone("+21620000009").build();
                Athlete a10 = Athlete.builder().fullName("Karim Haddad").photoUrl("https://ui-avatars.com/api/?name=Karim+Haddad&background=7C3AED&color=fff").dateOfBirth(LocalDate.parse("1999-09-18")).gender("Male").nationality("Tunisian").clubAffiliation("Club Africain").contactPhone("+21620000010").build();
                athleteRepository.saveAll(List.of(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10));

                rankingRepository.saveAll(List.of(
                                Ranking.builder().athlete(a1).category("Freestyle Distance").points(945).rankPosition(1).build(),
                                Ranking.builder().athlete(a2).category("Breaststroke").points(870).rankPosition(2).build(),
                                Ranking.builder().athlete(a3).category("Butterfly").points(730).rankPosition(6).build(),
                                Ranking.builder().athlete(a4).category("Backstroke").points(810).rankPosition(4).build(),
                                Ranking.builder().athlete(a5).category("Sprint Freestyle").points(760).rankPosition(5).build(),
                                Ranking.builder().athlete(a6).category("Backstroke").points(720).rankPosition(7).build(),
                                Ranking.builder().athlete(a7).category("Sprint Freestyle").points(890).rankPosition(3).build(),
                                Ranking.builder().athlete(a8).category("Breaststroke").points(680).rankPosition(9).build(),
                                Ranking.builder().athlete(a9).category("Butterfly").points(850).rankPosition(3).build(),
                                Ranking.builder().athlete(a10).category("Medley").points(920).rankPosition(2).build()
                ));

                licenseRepository.saveAll(List.of(
                                License.builder().athlete(a1).licenseNumber("TN-ATH-001").validFrom(LocalDate.now().minusMonths(3)).validTo(LocalDate.now().plusMonths(9)).status("ACTIVE").build(),
                                License.builder().athlete(a2).licenseNumber("TN-ATH-002").validFrom(LocalDate.now().minusMonths(2)).validTo(LocalDate.now().plusMonths(10)).status("ACTIVE").build(),
                                License.builder().athlete(a3).licenseNumber("TN-ATH-003").validFrom(LocalDate.now().minusMonths(1)).validTo(LocalDate.now().plusMonths(11)).status("ACTIVE").build(),
                                License.builder().athlete(a4).licenseNumber("TN-ATH-004").validFrom(LocalDate.now().minusMonths(4)).validTo(LocalDate.now().plusMonths(8)).status("ACTIVE").build(),
                                License.builder().athlete(a5).licenseNumber("TN-ATH-005").validFrom(LocalDate.now().minusMonths(5)).validTo(LocalDate.now().plusMonths(7)).status("ACTIVE").build(),
                                License.builder().athlete(a6).licenseNumber("TN-ATH-006").validFrom(LocalDate.now().minusMonths(2)).validTo(LocalDate.now().plusMonths(10)).status("ACTIVE").build(),
                                License.builder().athlete(a7).licenseNumber("TN-ATH-007").validFrom(LocalDate.now().minusMonths(1)).validTo(LocalDate.now().plusMonths(11)).status("ACTIVE").build(),
                                License.builder().athlete(a8).licenseNumber("TN-ATH-008").validFrom(LocalDate.now().minusMonths(6)).validTo(LocalDate.now().plusMonths(6)).status("ACTIVE").build(),
                                License.builder().athlete(a9).licenseNumber("TN-ATH-009").validFrom(LocalDate.now().minusMonths(1)).validTo(LocalDate.now().plusMonths(11)).status("ACTIVE").build(),
                                License.builder().athlete(a10).licenseNumber("TN-ATH-010").validFrom(LocalDate.now().minusMonths(3)).validTo(LocalDate.now().plusMonths(9)).status("ACTIVE").build()
                ));

                resultRepository.saveAll(List.of(
                                CompetitionResult.builder().athlete(a1).eventName("400m Freestyle").recordTime("03:46.21").rankPosition(1).points(945).build(),
                                CompetitionResult.builder().athlete(a1).eventName("800m Freestyle").recordTime("07:58.44").rankPosition(1).points(930).build(),
                                CompetitionResult.builder().athlete(a2).eventName("200m Breaststroke").recordTime("02:29.19").rankPosition(2).points(870).build(),
                                CompetitionResult.builder().athlete(a3).eventName("100m Butterfly").recordTime("00:54.10").rankPosition(6).points(730).build(),
                                CompetitionResult.builder().athlete(a4).eventName("100m Backstroke").recordTime("01:02.03").rankPosition(4).points(810).build(),
                                CompetitionResult.builder().athlete(a5).eventName("50m Freestyle").recordTime("00:22.96").rankPosition(5).points(760).build(),
                                CompetitionResult.builder().athlete(a6).eventName("200m Backstroke").recordTime("02:08.55").rankPosition(7).points(720).build(),
                                CompetitionResult.builder().athlete(a7).eventName("50m Freestyle").recordTime("00:24.81").rankPosition(3).points(890).build(),
                                CompetitionResult.builder().athlete(a8).eventName("100m Breaststroke").recordTime("01:04.32").rankPosition(9).points(680).build(),
                                CompetitionResult.builder().athlete(a9).eventName("200m Butterfly").recordTime("02:10.44").rankPosition(3).points(850).build(),
                                CompetitionResult.builder().athlete(a10).eventName("200m Individual Medley").recordTime("02:00.17").rankPosition(2).points(920).build()
                ));
        }

        private void seedSponsorPreferences() {
                sponsorRepository.findAll().forEach(sponsor -> {
                        sponsorPreferencesRepository.findBySponsorId(sponsor.getId()).ifPresentOrElse(existing -> {}, () -> {
                                SponsorPreferences preferences = SponsorPreferences.builder()
                                                .sponsor(sponsor)
                                                .preferredDisciplines(parseDisciplines(sponsor.getPreferredDisciplines()))
                                                .minRankPosition(sponsor.getTier() != null && sponsor.getTier().equalsIgnoreCase("PLATINUM") ? 3 : 10)
                                                .maxAthleteAge(28)
                                                .targetGender(null)
                                                .minFINAPoints(650)
                                                .geographicPreference("National")
                                                .contractValueRangeMin(5000.0)
                                                .contractValueRangeMax(sponsor.getTotalValue() != null ? sponsor.getTotalValue() : 50000.0)
                                                .build();
                                sponsorPreferencesRepository.save(preferences);
                        });
                });

                if (matchRepository.count() == 0 && athleteRepository.count() > 0) {
                        smartMatchingService.generateMatches(new com.ftn.platform.dto.MatchGenerationRequestDTO(null, 3, true));
                }
        }

        private List<String> parseDisciplines(String json) {
                try {
                        if (json == null || json.isBlank()) return List.of();
                        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,
                                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                } catch (Exception ex) {
                        return List.of();
                }
        }

    private void seedRequests() {
        SponsorshipRequest r1 = SponsorshipRequest.builder()
                .id("REQ-001")
                .applicant("Ahmed Hafnaoui")
                .applicantLogo("https://ui-avatars.com/api/?name=Ahmed+Hafnaoui&background=random")
                .type("ATHLETE")
                .sponsorName("Speedo")
                .requestDate(LocalDate.parse("2026-06-15"))
                .amount(15000.0)
                .priority("HIGH")
                .status(RequestStatus.UNDER_REVIEW)
                .build();

        SponsorshipRequest r2 = SponsorshipRequest.builder()
                .id("REQ-002")
                .applicant("Club Africain")
                .applicantLogo("https://ui-avatars.com/api/?name=Club+Africain&background=random")
                .type("CLUB")
                .sponsorName("Ooredoo Tunisia")
                .requestDate(LocalDate.parse("2026-06-18"))
                .amount(50000.0)
                .priority("HIGH")
                .status(RequestStatus.APPROVED)
                .build();

        SponsorshipRequest r3 = SponsorshipRequest.builder()
                .id("REQ-003")
                .applicant("National Junior Championship")
                .applicantLogo("https://ui-avatars.com/api/?name=National+Junior+Championship&background=random")
                .type("EVENT")
                .sponsorName("Arena")
                .requestDate(LocalDate.parse("2026-06-10"))
                .amount(25000.0)
                .priority("LOW")
                .status(RequestStatus.REJECTED)
                .build();

        SponsorshipRequest r4 = SponsorshipRequest.builder()
                .id("REQ-004")
                .applicant("Esperance Sportive")
                .applicantLogo("https://ui-avatars.com/api/?name=Esperance+Sportive&background=random")
                .type("CLUB")
                .sponsorName("Tunisair")
                .requestDate(LocalDate.parse("2026-06-19"))
                .amount(35000.0)
                .priority("MEDIUM")
                .status(RequestStatus.PENDING)
                .build();

        requestRepository.saveAll(List.of(r1, r2, r3, r4));
    }

    private void seedMatches() {
        AthleteMatch m1 = AthleteMatch.builder()
                .athleteName("Ahmed Hafnaoui")
                .discipline("Freestyle Distance")
                .rank(1)
                .suggestedSponsor("Speedo")
                .matchScore(95)
                .reason("Top performer in freestyle, aligns perfectly with Speedo performance brand goals.")
                .status(MatchStatus.PROPOSED)
                .build();

        AthleteMatch m2 = AthleteMatch.builder()
                .athleteName("Rami Chammari")
                .discipline("Butterfly")
                .rank(5)
                .suggestedSponsor("Arena")
                .matchScore(82)
                .reason("Rising star in butterfly events. Arena is looking to sponsor junior talent.")
                .status(MatchStatus.PROPOSED)
                .build();

        AthleteMatch m3 = AthleteMatch.builder()
                .athleteName("Sarra Lajnef")
                .discipline("Breaststroke")
                .rank(2)
                .suggestedSponsor("Ooredoo Tunisia")
                .matchScore(78)
                .reason("Strong local presence and high engagement on social media.")
                .status(MatchStatus.PROPOSED)
                .build();

        matchRepository.saveAll(List.of(m1, m2, m3));
    }

    private void seedEvents() {
        CompetitionEvent e1 = CompetitionEvent.builder()
                .name("National Summer Championship 2026")
                .bannerUrl("https://images.unsplash.com/photo-1530549387789-4c1017266635?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1170&q=80")
                .eventDate("Aug 15 - Aug 20, 2026")
                .location("Rades Olympic Pool")
                .athletesCount(450)
                .clubsCount(32)
                .audience("10,000+")
                .streaming(true)
                .revenueTarget(150000.0)
                .build();

        e1.getPackages().add(SponsorshipPackage.builder()
                .competitionEvent(e1).title("Title Sponsor").price("50,000 TND").benefits("Naming rights, Logo on pool deck, TV ads")
                .status(PackageStatus.AVAILABLE).build());
        e1.getPackages().add(SponsorshipPackage.builder()
                .competitionEvent(e1).title("Gold Tier").price("20,000 TND").benefits("Banners, Medals presentation, Web logo")
                .status(PackageStatus.TAKEN).sponsorName("Ooredoo").build());
        e1.getPackages().add(SponsorshipPackage.builder()
                .competitionEvent(e1).title("Silver Tier").price("10,000 TND").benefits("Web logo, Program ad, 2 VIP tickets")
                .status(PackageStatus.RESERVED).build());
        e1.getPackages().add(SponsorshipPackage.builder()
                .competitionEvent(e1).title("Equipment Partner").price("In-kind").benefits("Exclusive equipment provider")
                .status(PackageStatus.AVAILABLE).build());

        CompetitionEvent e2 = CompetitionEvent.builder()
                .name("Tunis Junior Open")
                .bannerUrl("https://images.unsplash.com/photo-1519315901367-f34ff91544b7?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1173&q=80")
                .eventDate("Sep 05 - Sep 07, 2026")
                .location("El Menzah Pool")
                .athletesCount(280)
                .clubsCount(15)
                .audience("3,500")
                .streaming(false)
                .revenueTarget(40000.0)
                .build();

        e2.getPackages().add(SponsorshipPackage.builder()
                .competitionEvent(e2).title("Main Event Sponsor").price("15,000 TND").benefits("Primary logo placement, Awards presentation")
                .status(PackageStatus.AVAILABLE).build());
        e2.getPackages().add(SponsorshipPackage.builder()
                .competitionEvent(e2).title("Media Partner").price("5,000 TND").benefits("Press backdrop logo, Program ad")
                .status(PackageStatus.AVAILABLE).build());
        e2.getPackages().add(SponsorshipPackage.builder()
                .competitionEvent(e2).title("Equipment Partner").price("In-kind").benefits("Swim caps & gear")
                .status(PackageStatus.TAKEN).sponsorName("Arena").build());

        eventRepository.saveAll(List.of(e1, e2));
    }

    private void seedSuggestions() {
        suggestionRepository.saveAll(List.of(
            Suggestion.builder().text("What are the qualifying times for the National Summer Championship?").category("competition").frequency(0).build(),
            Suggestion.builder().text("Tell me about Ahmed Hafnaoui's Olympic medals.").category("general").frequency(0).build(),
            Suggestion.builder().text("How do I renew my swimming license?").category("license").frequency(0).build(),
            Suggestion.builder().text("Who are the Platinum sponsors of the FTN?").category("general").frequency(0).build(),
            Suggestion.builder().text("When does the National Summer Championship registration close?").category("competition").frequency(0).build(),
            Suggestion.builder().text("What documents are needed for license renewal?").category("license").frequency(0).build(),
            Suggestion.builder().text("How do I check my FINA points?").category("competition").frequency(0).build(),
            Suggestion.builder().text("Which clubs are registered with the FTN?").category("general").frequency(0).build()
        ));
    }
}
