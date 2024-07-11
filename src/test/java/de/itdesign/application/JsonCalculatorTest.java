package de.itdesign.application;


import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * Test class to check correct data processing
 */
public class JsonCalculatorTest {

    private JSONArray data;
    private JSONArray operations;

    /**
     * Class for creating test data and operations with it
     * @throws IOException - class for handling exceptions when performing I/O operations
     */
    @Before
    public void setup() throws IOException {
        // Reading JSON files
        String dataContent = new String(Files.readAllBytes(Paths.get("data.json")));
        String operationsContent = new String(Files.readAllBytes(Paths.get("operations.json")));

        // Parsing the contents of JSON files into objects
        JSONObject dataJson = new JSONObject(dataContent);
        JSONObject operationsJson = new JSONObject(operationsContent);

        // Extracting arrays from JSON objects
        data = dataJson.getJSONArray("entries");
        operations = operationsJson.getJSONArray("operations");
    }

    /**
     * Test method for checking the results of test operations with test data
     */
    @Test
    public void processingTest() {
        // Data and Operation Processing
        JSONArray results = JsonCalculator.processing(data, operations);

        // Checking the results
        assertEquals(4, results.length());
        JSONObject result1 = results.getJSONObject(0);
        assertEquals("important", result1.getString("name"));
        assertEquals(4030418.67, result1.getDouble("roundedValue"), 0.01);
        JSONObject result2 = results.getJSONObject(1);
        assertEquals("information", result2.getString("name"));
        assertEquals(836.02, result2.getDouble("roundedValue"), 0.01);
        JSONObject result3 = results.getJSONObject(2);
        assertEquals("for", result3.getString("name"));
        assertEquals(80.76, result3.getDouble("roundedValue"), 0.01);
        JSONObject result4 = results.getJSONObject(3);
        assertEquals("future", result4.getString("name"));
        assertEquals(3440441.00, result4.getDouble("roundedValue"), 0.01);
    }
}