package de.itdesign.application;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JsonCalculator2 {
    public static void main(String[] args) {
        // Don't change this part
        if (args.length == 3) {
            // Path to the data file, e.g. "data.json"
            final String DATA_FILE = args[0];
            // Path to the operations file, e.g. "operations.json"
            final String OPERATIONS_FILE = args[1];
            // Path to the output file, e.g. "my-output.json"
            final String OUTPUT_FILE = args[2];

            try {
                // Reading JSON files and parsing the contents into objects
                JSONObject dataJson = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE))));
                JSONObject operationsJson = new JSONObject(new String(Files.readAllBytes(Paths.get(OPERATIONS_FILE))));

                // Extracting arrays from JSON objects
                JSONArray data = dataJson.getJSONArray("entries");
                JSONArray operations = operationsJson.getJSONArray("operations");

                // Processing data and operations
                JSONArray results = process(data, operations);

                // Writing results to a JSON file
                Files.write(Paths.get(OUTPUT_FILE), results.toString(2).getBytes());
            } catch (IOException e) {
                System.out.println("Error processing: " + e.getMessage());
            }
        } else {
            System.exit(1);
        }
    }

    // Method for processing data and operations
    static JSONArray process(JSONArray entries, JSONArray operations) {
        JSONArray results = new JSONArray();

        // Iterate over operations
        for (int i = 0; i < operations.length(); i++) {
            JSONObject operation = operations.getJSONObject(i);
            String name = operation.optString("name");
            String function = operation.optString("function");
            String filter = operation.optString("filter");
            JSONArray fields = operation.optJSONArray("field");
            if (fields == null) {
                throw new IllegalArgumentException("Operation must have a field array");
            }

            // Compiling a regular expression for filtering
            Pattern pattern = Pattern.compile(filter);
            List<JSONObject> filteredEntries = IntStream.range(0, entries.length())
                    .mapToObj(entries::getJSONObject)
                    .filter(entry -> pattern.matcher(entry.getString("name")).matches())
                    .collect(Collectors.toList());

            // Calculating the result
            double result = calculate(function, filteredEntries, fields);

            // Create a result object and add it to the results array
            JSONObject resultObject = new JSONObject();
            resultObject.put("name", name);
            resultObject.put("roundedValue", formatDouble(result));
            results.put(resultObject);
        }

        return results;
    }

    // Formatting the added value
    private static String formatDouble(double value) {
        DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(value);
    }

    // Method for calculating the result of an operation
    private static double calculate(String function, List<JSONObject> entries, JSONArray fields) {
        // Convert field values to a list of values
        List<Double> values = entries.stream()
                .map(entry -> extractValues(entry, fields))
                .toList();

        // Calculating the result
        return switch (function) {
            case "min" -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            case "max" -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            case "sum" -> values.stream().mapToDouble(Double::doubleValue).sum();
            case "average" -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            default -> throw new IllegalArgumentException("Unknown function: " + function);
        };
    }

    // Method to extract field values from a JSON object, supporting both string and array paths
    private static double extractValues(JSONObject entry, JSONArray fields) {
        return IntStream.range(0, fields.length())
                .mapToDouble(i -> {
                    Object field = fields.get(i);
                    String path;
                    if (field instanceof String) {
                        path = (String) field;
                    } else if (field instanceof JSONArray) {
                        path = String.join(".", toArray((JSONArray) field));
                    } else {
                        throw new IllegalArgumentException("Unexpected field type: " + field.getClass().getName());
                    }

                    Object value = entry;
                    for (String key : path.split("\\.")) {
                        if (value instanceof JSONObject) {
                            value = ((JSONObject) value).get(key);
                        } else {
                            throw new IllegalArgumentException("Unexpected type: " + value.getClass().getName());
                        }
                    }
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    } else {
                        throw new IllegalArgumentException("Value is not a number: " + value.getClass().getName());
                    }
                })
                .sum();
    }

    // Helper method to convert JSONArray to String array
    private static String[] toArray(JSONArray array) {
        String[] result = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            result[i] = array.getString(i);
        }
        return result;
    }
}
