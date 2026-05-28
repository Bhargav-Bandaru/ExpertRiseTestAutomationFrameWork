package com.expertrise.automation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * UserResponsePojo — deserialises the User API response body.
 *
 * Matches the GET/POST/PATCH /users response from User.json:
 * {
 *   "id": "1",
 *   "firstName": "Jane",
 *   "lastName": "Doe",
 *   "email": "jane.doe@test.com",
 *   "role": "user"
 * }
 *
 * Usage with RestAssured (cleaner than jsonPath().getString("email")):
 *   UserResponsePojo user = given()
 *       .body(requestPojo)
 *       .when().post("/users")
 *       .then().statusCode(201)
 *       .extract().as(UserResponsePojo.class);
 *
 *   Assertions.assertEquals("jane.doe@test.com", user.getEmail());
 *   Assertions.assertNotNull(user.getId());
 *
 *   // Store id for DELETE/GET/PATCH in next steps
 *   String createdId = user.getId();
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponsePojo {

    @JsonProperty("id")        private String id;
    @JsonProperty("firstName") private String firstName;
    @JsonProperty("lastName")  private String lastName;
    @JsonProperty("email")     private String email;
    @JsonProperty("role")      private String role;

    // ── Constructor ───────────────────────────────────────────────────────────

    public UserResponsePojo() {}

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()        { return id;        }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName;  }
    public String getEmail()     { return email;     }
    public String getRole()      { return role;      }

    // ── Fluent setters ────────────────────────────────────────────────────────

    public UserResponsePojo setId(String v)        { id        = v; return this; }
    public UserResponsePojo setFirstName(String v) { firstName = v; return this; }
    public UserResponsePojo setLastName(String v)  { lastName  = v; return this; }
    public UserResponsePojo setEmail(String v)     { email     = v; return this; }
    public UserResponsePojo setRole(String v)      { role      = v; return this; }

    /** Convenience — firstName + " " + lastName combined. */
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    @Override
    public String toString() {
        return "UserResponsePojo{id='" + id + "', firstName='" + firstName +
               "', lastName='" + lastName + "', email='" + email + "', role='" + role + "'}";
    }
}
