package com.expertrise.automation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * UserRequestPojo — request body POJO for the User API.
 *
 * Matches User.json and the existing ApiStepDefinitions.
 * {
 *   "firstName": "Jane",
 *   "lastName": "Doe",
 *   "email": "jane.doe@test.com",
 *   "role": "user"
 * }
 *
 * Usage with RestAssured (replaces String.format JSON building):
 *   UserRequestPojo req = UserRequestPojo.create("Jane","Doe","jane@test.com","user");
 *   given().body(req).when().post("/users").then().statusCode(201);
 *
 *   // PATCH — only email field sent (other fields null → excluded by @JsonInclude)
 *   UserRequestPojo patch = UserRequestPojo.patchEmail("new@test.com");
 *   given().body(patch).when().patch("/users/1").then().statusCode(200);
 */
@JsonInclude(JsonInclude.Include.NON_NULL)   // null fields excluded from JSON output
@JsonIgnoreProperties(ignoreUnknown = true) // extra response fields don't cause errors
public class UserRequestPojo {

    @JsonProperty("firstName") private String firstName;
    @JsonProperty("lastName")  private String lastName;
    @JsonProperty("email")     private String email;
    @JsonProperty("role")      private String role;

    // ── Constructors ──────────────────────────────────────────────────────────

    public UserRequestPojo() {}

    public UserRequestPojo(String firstName, String lastName, String email, String role) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.email     = email;
        this.role      = role;
    }

    // ── Static Factories ──────────────────────────────────────────────────────

    /** Build a full user creation request. */
    public static UserRequestPojo create(String firstName, String lastName,
                                         String email, String role) {
        return new UserRequestPojo(firstName, lastName, email, role);
    }

    /**
     * Build a partial PATCH request with only the email field.
     * Produces: { "email": "new@test.com" }
     * All other fields remain null and are excluded from the JSON body.
     */
    public static UserRequestPojo patchEmail(String newEmail) {
        UserRequestPojo p = new UserRequestPojo();
        p.email = newEmail;
        return p;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName;  }
    public String getEmail()     { return email;     }
    public String getRole()      { return role;      }

    // ── Fluent setters ────────────────────────────────────────────────────────

    public UserRequestPojo setFirstName(String v) { firstName = v; return this; }
    public UserRequestPojo setLastName(String v)  { lastName  = v; return this; }
    public UserRequestPojo setEmail(String v)     { email     = v; return this; }
    public UserRequestPojo setRole(String v)      { role      = v; return this; }

    @Override
    public String toString() {
        return "UserRequestPojo{firstName='" + firstName + "', lastName='" + lastName +
               "', email='" + email + "', role='" + role + "'}";
    }
}
