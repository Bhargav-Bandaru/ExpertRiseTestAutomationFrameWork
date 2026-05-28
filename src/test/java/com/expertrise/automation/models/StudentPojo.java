package com.expertrise.automation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * StudentPojo — maps to the existing students.json data file.
 *
 * Single student object structure:
 * {
 *   "id": 1,
 *   "name": "John Doe",
 *   "age": 18,
 *   "grade": "12th",
 *   "subjects": ["Math", "Physics", "English"]
 * }
 *
 * Read full students.json using the inner StudentsWrapper class:
 *   StudentsWrapper wrapper = JsonUtil.readFromFile("students.json", StudentsWrapper.class);
 *   List&lt;StudentPojo&gt; all = wrapper.getStudents();
 *   StudentPojo first = all.get(0);
 *   System.out.println(first.getName());  // "John Doe"
 *
 * Or deserialise a single student from API response:
 *   StudentPojo s = response.as(StudentPojo.class);
 *   Assertions.assertEquals(18, s.getAge());
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentPojo {

    @JsonProperty("id")       private int          id;
    @JsonProperty("name")     private String       name;
    @JsonProperty("age")      private int          age;
    @JsonProperty("grade")    private String       grade;
    @JsonProperty("subjects") private List<String> subjects;

    // ── Constructors ──────────────────────────────────────────────────────────

    public StudentPojo() {}

    public StudentPojo(int id, String name, int age, String grade, List<String> subjects) {
        this.id       = id;
        this.name     = name;
        this.age      = age;
        this.grade    = grade;
        this.subjects = subjects;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int          getId()       { return id;       }
    public String       getName()     { return name;     }
    public int          getAge()      { return age;      }
    public String       getGrade()    { return grade;    }
    public List<String> getSubjects() { return subjects; }

    // ── Fluent setters ────────────────────────────────────────────────────────

    public StudentPojo setId(int v)               { id       = v; return this; }
    public StudentPojo setName(String v)          { name     = v; return this; }
    public StudentPojo setAge(int v)              { age      = v; return this; }
    public StudentPojo setGrade(String v)         { grade    = v; return this; }
    public StudentPojo setSubjects(List<String> v){ subjects = v; return this; }

    @Override
    public String toString() {
        return "StudentPojo{id=" + id + ", name='" + name + "', age=" + age +
               ", grade='" + grade + "', subjects=" + subjects + '}';
    }

    // ── Inner wrapper for full students.json file ─────────────────────────────

    /**
     * Wrapper for the top-level {"students":[...]} structure in students.json.
     *
     * Usage:
     *   StudentsWrapper w = JsonUtil.readFromFile("students.json", StudentsWrapper.class);
     *   List&lt;StudentPojo&gt; list = w.getStudents();
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StudentsWrapper {
        @JsonProperty("students")
        private List<StudentPojo> students;

        public StudentsWrapper() {}
        public List<StudentPojo> getStudents() { return students; }
        public void setStudents(List<StudentPojo> s) { students = s; }
    }
}
