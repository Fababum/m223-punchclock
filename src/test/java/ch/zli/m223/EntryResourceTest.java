package ch.zli.m223;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class EntryResourceTest {

    @Test
    public void testEntryEndpoints() {
        given()
          .when().get("/entries")
          .then()
             .statusCode(200)
             .body(is("[]"));

        Long id = given()
          .contentType("application/json")
          .body("""
              {"checkIn":"2026-09-29T08:00:00","checkOut":"2026-09-29T17:00:00"}
              """)
          .when().post("/entries")
          .then()
             .statusCode(200)
             .body("checkIn", is("2026-09-29T08:00:00"))
             .body("checkOut", is("2026-09-29T17:00:00"))
             .extract().jsonPath().getLong("id");

        given()
          .when().get("/entries")
          .then()
             .statusCode(200)
             .body("size()", is(1))
             .body("[0].id", is(id.intValue()))
             .body("[0].checkIn", is("2026-09-29T08:00:00"))
             .body("[0].checkOut", is("2026-09-29T17:00:00"));

        given()
          .contentType("application/json")
          .body("""
              {"checkIn":"2026-09-29T09:00:00","checkOut":"2026-09-29T18:00:00"}
              """)
          .when().put("/entries/{id}", id)
          .then()
             .statusCode(200)
             .body("id", is(id.intValue()))
             .body("checkIn", is("2026-09-29T09:00:00"))
             .body("checkOut", is("2026-09-29T18:00:00"));

        given()
          .contentType("application/json")
          .body("""
              {"checkIn":"2026-09-29T09:00:00","checkOut":"2026-09-29T18:00:00"}
              """)
          .when().put("/entries/{id}", id + 1000)
          .then()
             .statusCode(404);

        given()
          .when().delete("/entries/{id}", id)
          .then()
             .statusCode(204);

        given()
          .when().get("/entries")
          .then()
             .statusCode(200)
             .body("size()", is(0));
    }

    @Test
    public void testValidation() {
        given()
          .contentType("application/json")
          .body("""
              {"checkOut":"2026-09-29T17:00:00"}
              """)
          .when().post("/entries")
          .then()
             .statusCode(400);

        given()
          .contentType("application/json")
          .body("""
              {"checkIn":"2026-09-29T08:00:00"}
              """)
          .when().post("/entries")
          .then()
             .statusCode(400);

        given()
          .contentType("application/json")
          .body("""
              {"checkIn":"2026-09-29T17:00:00","checkOut":"2026-09-29T08:00:00"}
              """)
          .when().post("/entries")
          .then()
             .statusCode(400);

        given()
          .contentType("application/json")
          .body("""
              {"checkIn":"2026-09-29T17:00:00","checkOut":"2026-09-29T08:00:00"}
              """)
          .when().put("/entries/1")
          .then()
             .statusCode(400);
    }

}