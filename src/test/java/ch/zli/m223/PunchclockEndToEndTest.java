package ch.zli.m223;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.zli.m223.repository.ApplicationUserRepository;
import ch.zli.m223.repository.CategoryRepository;
import ch.zli.m223.repository.EntryRepository;
import ch.zli.m223.repository.TagRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class PunchclockEndToEndTest {

    @Inject
    EntryRepository entryRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    TagRepository tagRepository;

    @Inject
    ApplicationUserRepository userRepository;

    @BeforeEach
    @AfterEach
    @Transactional
    void cleanDatabase() {
        entryRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.delete("email != ?1", "admin@example.com");
    }

    @Test
    void fullUserAndAdminWorkflow_fromRegistrationToTimeSummaryAndDeletion() {
        // 0. Statisches Frontend unter / ist öffentlich erreichbar
        given()
            .when().get("/")
            .then()
                .statusCode(200);

        // 1. Unauthentifizierter Zugriff auf /entries wird abgelehnt
        given()
            .when().get("/entries")
            .then()
                .statusCode(401);

        // 2. Neuer Benutzer registriert sich über die API und wird in der Datenbank gespeichert
        String userCredentials = """
                {"email":"employee@example.com","password":"userPass123"}
                """;
        given()
            .contentType(ContentType.JSON)
            .body(userCredentials)
            .when().post("/auth/register")
            .then()
                .statusCode(201);
        assertNotNull(userRepository.findByEmail("employee@example.com"));

        // 3. Benutzer loggt sich ein und erhält ein echtes signiertes JWT
        String userToken = given()
            .contentType(ContentType.JSON)
            .body(userCredentials)
            .when().post("/auth/login")
            .then()
                .statusCode(200)
                .extract().asString();

        // 4. Benutzer erstellt Kategorie und Tag mit seinem Bearer-Token
        Long categoryId = given()
            .header("Authorization", "Bearer " + userToken)
            .contentType(ContentType.JSON)
            .body("""
                    {"categoryName":"Kundenprojekt"}
                    """)
            .when().post("/categories")
            .then()
                .statusCode(200)
                .body("categoryName", is("Kundenprojekt"))
                .extract().jsonPath().getLong("id");

        Long tagId = given()
            .header("Authorization", "Bearer " + userToken)
            .contentType(ContentType.JSON)
            .body("""
                    {"title":"Backend"}
                    """)
            .when().post("/tags")
            .then()
                .statusCode(200)
                .body("title", is("Backend"))
                .extract().jsonPath().getLong("id");

        assertNotNull(categoryRepository.findById(categoryId));
        assertNotNull(tagRepository.findById(tagId));

        // 5. Benutzer erfasst zwei Zeiteinträge über POST /entries (inkl. Kategorie & Tag)
        Long morningEntryId = given()
            .header("Authorization", "Bearer " + userToken)
            .contentType(ContentType.JSON)
            .body("""
                    {"checkIn":"2026-10-05T08:00:00","checkOut":"2026-10-05T12:00:00","categoryId":%d,"tagIds":[%d]}
                    """.formatted(categoryId, tagId))
            .when().post("/entries")
            .then()
                .statusCode(200)
                .body("categoryId", is(categoryId.intValue()))
                .body("tagIds[0]", is(tagId.intValue()))
                .extract().jsonPath().getLong("id");

        Long afternoonEntryId = given()
            .header("Authorization", "Bearer " + userToken)
            .contentType(ContentType.JSON)
            .body("""
                    {"checkIn":"2026-10-05T13:00:00","checkOut":"2026-10-05T17:00:00"}
                    """)
            .when().post("/entries")
            .then()
                .statusCode(200)
                .extract().jsonPath().getLong("id");

        // 6. Prüfen, dass beide Einträge über GET /entries abrufbar UND in der Datenbank gespeichert sind
        given()
            .header("Authorization", "Bearer " + userToken)
            .when().get("/entries")
            .then()
                .statusCode(200)
                .body("size()", is(2));
        assertEquals(2, entryRepository.count());

        // 7. Benutzer korrigiert den Nachmittagseintrag (+1h 30m) und prüft die Tagesstatistik
        given()
            .header("Authorization", "Bearer " + userToken)
            .contentType(ContentType.JSON)
            .body("""
                    {"checkIn":"2026-10-05T13:00:00","checkOut":"2026-10-05T18:30:00"}
                    """)
            .when().put("/entries/{id}", afternoonEntryId)
            .then()
                .statusCode(200)
                .body("checkOut", is("2026-10-05T18:30:00"));

        given()
            .header("Authorization", "Bearer " + userToken)
            .when().get("/statistics/time-summaries")
            .then()
                .statusCode(200)
                .body("'2026-10-05'", is("09:30:00"));

        // 8. Autorisierung prüfen: Rolle User darf keine Entries löschen (403 Forbidden)
        given()
            .header("Authorization", "Bearer " + userToken)
            .when().delete("/entries/{id}", morningEntryId)
            .then()
                .statusCode(403);
        assertNotNull(entryRepository.findById(morningEntryId));

        // 9. Admin loggt sich ein, löscht den Eintrag erfolgreich (204) und Datenbank + API sind konsistent
        String adminToken = given()
            .contentType(ContentType.JSON)
            .body("""
                    {"email":"admin@example.com","password":"admin123"}
                    """)
            .when().post("/auth/login")
            .then()
                .statusCode(200)
                .extract().asString();

        given()
            .header("Authorization", "Bearer " + adminToken)
            .when().delete("/entries/{id}", morningEntryId)
            .then()
                .statusCode(204);

        entryRepository.getEntityManager().clear();
        assertNull(entryRepository.findById(morningEntryId));
        assertEquals(1, entryRepository.count());

        given()
            .header("Authorization", "Bearer " + userToken)
            .when().get("/entries")
            .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(afternoonEntryId.intValue()));
    }
}
