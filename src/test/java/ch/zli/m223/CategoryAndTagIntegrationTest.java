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

import ch.zli.m223.model.Category;
import ch.zli.m223.model.Tag;
import ch.zli.m223.repository.CategoryRepository;
import ch.zli.m223.repository.TagRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

@QuarkusTest
@TestSecurity(user = "user@example.com", roles = "User")
public class CategoryAndTagIntegrationTest {

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    TagRepository tagRepository;

    @BeforeEach
    @AfterEach
    @Transactional
    void cleanDatabase() {
        tagRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void categoryCrud_validRequests_persistsUpdatesAndDeletesInDatabase() {
        // Given: Eine gültige Kategorie als JSON-Payload
        String createPayload = """
                {"categoryName":"Projektleitung"}
                """;

        // When: Kategorie über die REST-API erstellen
        Long categoryId = given()
            .contentType(ContentType.JSON)
            .body(createPayload)
            .when().post("/categories")
            .then()
                .statusCode(200)
                .body("categoryName", is("Projektleitung"))
                .extract().jsonPath().getLong("id");

        // Then: Kategorie ist tatsächlich in der H2-Testdatenbank gespeichert
        Category persisted = categoryRepository.findById(categoryId);
        assertNotNull(persisted);
        assertEquals("Projektleitung", persisted.getCategoryName());

        // When: Kategorie über PUT aktualisieren
        given()
            .contentType(ContentType.JSON)
            .body("""
                    {"categoryName":"Entwicklung"}
                    """)
            .when().put("/categories/{id}", categoryId)
            .then()
                .statusCode(200)
                .body("categoryName", is("Entwicklung"));

        // Then: Änderung ist in der Datenbank persistiert
        categoryRepository.getEntityManager().clear();
        assertEquals("Entwicklung", categoryRepository.findById(categoryId).getCategoryName());

        // When: Kategorie über DELETE löschen
        given()
            .when().delete("/categories/{id}", categoryId)
            .then()
                .statusCode(204);

        // Then: Kategorie existiert nicht mehr in der Datenbank
        categoryRepository.getEntityManager().clear();
        assertNull(categoryRepository.findById(categoryId));
    }

    @Test
    void createCategory_blankName_returnsBadRequestAndDoesNotPersist() {
        // Given: Ungültige Kategorie mit leerem Namen
        String invalidPayload = """
                {"categoryName":"   "}
                """;

        // When: POST an /categories senden
        given()
            .contentType(ContentType.JSON)
            .body(invalidPayload)
            .when().post("/categories")
            .then()
                // Then: Bean-Validation lehnt Request mit 400 ab und Datenbank bleibt leer
                .statusCode(400);

        assertEquals(0, categoryRepository.count());
    }

    @Test
    void tagCrud_validRequests_persistsUpdatesAndDeletesInDatabase() {
        // Given: Ein gültiger Tag als JSON-Payload
        String createPayload = """
                {"title":"Dringend"}
                """;

        // When: Tag über die REST-API erstellen
        Long tagId = given()
            .contentType(ContentType.JSON)
            .body(createPayload)
            .when().post("/tags")
            .then()
                .statusCode(200)
                .body("title", is("Dringend"))
                .extract().jsonPath().getLong("id");

        // Then: Tag ist in der H2-Testdatenbank gespeichert
        Tag persisted = tagRepository.findById(tagId);
        assertNotNull(persisted);
        assertEquals("Dringend", persisted.getTitle());

        // When: Tag über PUT umbenennen
        given()
            .contentType(ContentType.JSON)
            .body("""
                    {"title":"Kundenprojekt"}
                    """)
            .when().put("/tags/{id}", tagId)
            .then()
                .statusCode(200)
                .body("title", is("Kundenprojekt"));

        // Then: Neuer Titel ist in der Datenbank gespeichert
        tagRepository.getEntityManager().clear();
        assertEquals("Kundenprojekt", tagRepository.findById(tagId).getTitle());

        // When: Nicht existierender Tag wird gelöscht
        given()
            .when().delete("/tags/{id}", tagId + 999)
            .then()
                .statusCode(404);

        // When: Existierender Tag wird gelöscht
        given()
            .when().delete("/tags/{id}", tagId)
            .then()
                .statusCode(204);

        // Then: Tag ist aus der Datenbank entfernt
        tagRepository.getEntityManager().clear();
        assertNull(tagRepository.findById(tagId));
    }
}
