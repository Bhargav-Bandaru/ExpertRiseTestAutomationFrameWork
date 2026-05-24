package com.expertrise.automation.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

/**
 * JsonUtil — reusable JSON read/write utility backed by Jackson ObjectMapper.
 *
 * <p>Supports:
 * <ul>
 *   <li>Read JSON file into POJO / Map / List</li>
 *   <li>Write POJO / Map to JSON file</li>
 *   <li>Read/update specific fields in a JSON file</li>
 *   <li>Convert between JSON string and Java objects</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   // Read JSON file into POJO
 *   UserPojo user = JsonUtil.readFromFile("testdata/user.json", UserPojo.class);
 *
 *   // Read JSON file into generic Map
 *   Map<String, Object> data = JsonUtil.readAsMap("testdata/config.json");
 *
 *   // Write POJO to file
 *   JsonUtil.writeToFile("target/output/result.json", responseObject);
 *
 *   // Update a specific field in an existing JSON file
 *   JsonUtil.updateField("User.json", "email", "new@email.com");
 * </pre>
 */
public class JsonUtil {

    private static final Logger log = LogManager.getLogger(JsonUtil.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);   // pretty-print by default

    // ──────────────────────────────────────────────────────────────────────────
    // READ OPERATIONS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Reads a JSON file from the classpath or file system and deserialises it
     * into the specified POJO class.
     *
     * @param filePath path relative to project root OR classpath resource path
     * @param clazz    target class
     */
    public static <T> T readFromFile(String filePath, Class<T> clazz) {
        try {
            File file = resolveFile(filePath);
            T result = MAPPER.readValue(file, clazz);
            log.info("Read JSON from '{}' into {}", filePath, clazz.getSimpleName());
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + filePath, e);
        }
    }

    /**
     * Reads a JSON file into a generic {@code Map<String, Object>}.
     * Useful for dynamic JSON without a fixed POJO.
     */
    public static Map<String, Object> readAsMap(String filePath) {
        try {
            File file = resolveFile(filePath);
            Map<String, Object> result = MAPPER.readValue(file, new TypeReference<>() {});
            log.info("Read JSON map from '{}'", filePath);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON map from: " + filePath, e);
        }
    }

    /**
     * Reads a JSON file into a {@code List<T>} — for JSON arrays.
     */
    public static <T> List<T> readAsList(String filePath, Class<T> elementClass) {
        try {
            File file = resolveFile(filePath);
            List<T> result = MAPPER.readValue(file,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementClass));
            log.info("Read JSON list ({} items) from '{}'", result.size(), filePath);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON list from: " + filePath, e);
        }
    }

    /**
     * Reads a raw JSON file as a String (no parsing).
     */
    public static String readAsString(String filePath) {
        try {
            return Files.readString(resolveFile(filePath).toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file as string: " + filePath, e);
        }
    }

    /**
     * Parses a JSON string into the specified POJO class.
     *
     * @param jsonString valid JSON string
     * @param clazz      target POJO class
     */
    public static <T> T fromString(String jsonString, Class<T> clazz) {
        try {
            return MAPPER.readValue(jsonString, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON string into " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Reads a specific field value from a JSON file.
     *
     * @param filePath  path to JSON file
     * @param fieldName top-level field name
     * @return field value as String
     */
    public static String readField(String filePath, String fieldName) {
        try {
            JsonNode root = MAPPER.readTree(resolveFile(filePath));
            JsonNode field = root.get(fieldName);
            if (field == null) throw new RuntimeException(
                "Field '" + fieldName + "' not found in " + filePath);
            return field.asText();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read field from: " + filePath, e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // WRITE OPERATIONS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Serialises a Java object to a JSON file (pretty-printed).
     * Creates parent directories if they do not exist.
     *
     * @param filePath output file path
     * @param object   any Java object (POJO, Map, List)
     */
    public static void writeToFile(String filePath, Object object) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();  // create parent dirs if needed
            MAPPER.writeValue(file, object);
            log.info("Written JSON to '{}'", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON to: " + filePath, e);
        }
    }

    /**
     * Updates a specific top-level field in an existing JSON file.
     * Reads → modifies → writes back atomically.
     *
     * @param filePath   path to existing JSON file
     * @param fieldName  field to update
     * @param newValue   new value (as String)
     */
    public static void updateField(String filePath, String fieldName, String newValue) {
        try {
            File file = resolveFile(filePath);
            ObjectNode root = (ObjectNode) MAPPER.readTree(file);
            root.put(fieldName, newValue);
            MAPPER.writeValue(file, root);
            log.info("Updated field '{}' = '{}' in '{}'", fieldName, newValue, filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update field in: " + filePath, e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CONVERSION HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Converts any Java object to a pretty-printed JSON String.
     */
    public static String toJsonString(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialise object to JSON string", e);
        }
    }

    /**
     * Converts a Map to a compact (non-pretty) JSON String — useful for request bodies.
     */
    public static String mapToJson(Map<String, Object> map) {
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert map to JSON", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INTERNAL
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Resolves a file path: tries file system first, then classpath.
     */
    private static File resolveFile(String filePath) {
        // 1. Try direct file system path
        File file = new File(filePath);
        if (file.exists()) return file;

        // 2. Try classpath resource
        InputStream is = JsonUtil.class.getClassLoader().getResourceAsStream(filePath);
        if (is != null) {
            try {
                Path tmpFile = Files.createTempFile("json_util_", ".json");
                Files.copy(is, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                return tmpFile.toFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read classpath resource: " + filePath, e);
            }
        }

        throw new RuntimeException("File not found on filesystem or classpath: " + filePath);
    }

    private JsonUtil() { /* utility class */ }
}
