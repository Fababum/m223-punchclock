package ch.zli.m223;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.time.LocalDateTime;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.zli.m223.model.Entry;
import ch.zli.m223.repository.EntryRepository;
import ch.zli.m223.service.EntryService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
public class StatisticsIntegrationTest {

    @Inject
    EntryService entryService;

    @Inject
    EntryRepository entryRepository;

    @BeforeEach
    @AfterEach
    @Transactional
    void cleanDatabase() {
        entryRepository.deleteAll();
    }

    @Test
    @TestSecurity(user = "user@example.com", roles = "User")
    void timeSummaries_entriesInDatabase_returnsAggregatedDurationPerDay() {
        // Given: Mehrere Einträge über EntryService in der isolierten H2-Testdatenbank gespeichert
        entryService.createEntry(buildEntry(
                LocalDateTime.of(2026, 9, 29, 8, 0),
                LocalDateTime.of(2026, 9, 29, 12, 0)));
        entryService.createEntry(buildEntry(
                LocalDateTime.of(2026, 9, 29, 13, 0),
                LocalDateTime.of(2026, 9, 29, 17, 30)));
        entryService.createEntry(buildEntry(
                LocalDateTime.of(2026, 9, 30, 9, 0),
                LocalDateTime.of(2026, 9, 30, 12, 15)));

        // When & Then: Abruf über StatisticsController aggregiert die Zeiten pro Tag (Controller -> EntryService -> TimeSummaryService -> DB)
        given()
            .when().get("/statistics/time-summaries")
            .then()
                .statusCode(200)
                .body("'2026-09-29'", is("08:30:00"))
                .body("'2026-09-30'", is("03:15:00"));
    }

    @Test
    @TestSecurity(user = "user@example.com", roles = "User")
    void timeSummaries_emptyDatabase_returnsEmptyJson() {
        // Given: Leere Testdatenbank

        // When & Then: Statistik-Endpunkt liefert leeres JSON-Objekt
        given()
            .when().get("/statistics/time-summaries")
            .then()
                .statusCode(200)
                .body(is("{}"));
    }

    @Test
    void timeSummaries_unauthenticated_returnsUnauthorized() {
        // Given: Kein authentifizierter Benutzer

        // When & Then: Zugriff auf geschützten Statistik-Endpunkt wird mit 401 abgelehnt
        given()
            .when().get("/statistics/time-summaries")
            .then()
                .statusCode(401);
    }

    private static Entry buildEntry(LocalDateTime checkIn, LocalDateTime checkOut) {
        Entry entry = new Entry();
        entry.setCheckIn(checkIn);
        entry.setCheckOut(checkOut);
        return entry;
    }
}
