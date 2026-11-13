package ch.zli.m223;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.zli.m223.model.ApplicationUser;
import ch.zli.m223.model.Role;
import ch.zli.m223.repository.ApplicationUserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class AuthIntegrationTest {

    @Inject
    ApplicationUserRepository userRepository;

    @BeforeEach
    @AfterEach
    @Transactional
    void cleanTestUsers() {
        userRepository.delete("email != ?1", "admin@example.com");
    }

    @Test
    void registerAndLogin_validCredentials_persistsHashedUserAndReturnsJwt() {
        // Given: Zugangsdaten für einen neuen Benutzer
        String credentials = """
                {"email":"integration@example.com","password":"secretPassword123"}
                """;

        // When: Benutzer registriert sich über /auth/register
        given()
            .contentType(ContentType.JSON)
            .body(credentials)
            .when().post("/auth/register")
            .then()
                .statusCode(201);

        // Then: Benutzer ist mit Rolle User und gehashtem Passwort in der Datenbank gespeichert
        ApplicationUser savedUser = userRepository.findByEmail("integration@example.com");
        assertNotNull(savedUser);
        assertEquals(Role.User, savedUser.getRole());
        assertNotEquals("secretPassword123", savedUser.getPassword());

        // When: Benutzer loggt sich über /auth/login ein
        String jwt = given()
            .contentType(ContentType.JSON)
            .body(credentials)
            .when().post("/auth/login")
            .then()
                // Then: Ein signiertes JWT wird zurückgegeben
                .statusCode(200)
                .extract().asString();

        assertFalse(jwt.isBlank());
    }

    @Test
    void register_duplicateEmail_returnsConflict() {
        // Given: Ein bereits registrierter Benutzer
        String credentials = """
                {"email":"duplicate@example.com","password":"password123"}
                """;
        given()
            .contentType(ContentType.JSON)
            .body(credentials)
            .when().post("/auth/register")
            .then()
                .statusCode(201);

        // When & Then: Erneute Registrierung mit derselben E-Mail liefert 409 Conflict
        given()
            .contentType(ContentType.JSON)
            .body(credentials)
            .when().post("/auth/register")
            .then()
                .statusCode(409);
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() {
        // Given: Existierender Admin-Benutzer mit falschem Passwort
        String wrongCredentials = """
                {"email":"admin@example.com","password":"wrongPassword"}
                """;

        // When & Then: Login wird mit 401 Unauthorized abgewiesen
        given()
            .contentType(ContentType.JSON)
            .body(wrongCredentials)
            .when().post("/auth/login")
            .then()
                .statusCode(401);
    }
}
