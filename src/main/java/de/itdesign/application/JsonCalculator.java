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

/**
 * class for working with data and operations with them
 */
public class JsonCalculator {
    public static void main(String[] args) {
        // Don't change this part
        if (args.length == 3) {
            // Path to the data file, e.g. "data.json"
            final String DATA_FILE = args[0];
            // Path to the data file, e.g. "operations.json"
            final String OPERATIONS_FILE = args[1];
            // Path to the output file, e.g. "my-output.json"
            final String OUTPUT_FILE = args[2];

            // <your code here>
            try {
                // Reading JSON files and Parsing the contents of JSON files into objects
                JSONObject dataJson = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE))));
                JSONObject operationsJson = new JSONObject(new String(Files.readAllBytes(Paths.get(OPERATIONS_FILE))));

                // Extracting arrays from JSON objects
                JSONArray data = dataJson.getJSONArray("entries");
                JSONArray operations = operationsJson.getJSONArray("operations");

                // Data and Operation Processing
                JSONArray results = processing(data, operations);

                // Writing results to a JSON file
                Files.write(Paths.get(OUTPUT_FILE), results.toString(2).getBytes());
            } catch (IOException e) {
                System.out.println("Error processing: " + e.getMessage());
            }
        } else {
            System.exit(1);
        }
    }

    /**
     * Method for processing data and operations
     * @param entries - data array for processing
     * @param operations - array of data processing operations
     * @return - returns an array of processed data
     */
    static JSONArray processing(JSONArray entries, JSONArray operations) {
        JSONArray results = new JSONArray();

        // Iterate over operations
        IntStream.range(0, operations.length()).mapToObj(operations::getJSONObject).forEach(operation -> {
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
        });

        return results;
    }

    /**
     * Formatting the added value
     * @param value - value to format the number of characters to 2 after floating point
     * @return - returns a string value
     */
    private static String formatDouble(double value) {
        DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(value);
    }

    /**
     * Method for calculating the result of an operation
     * @param function - function for which calculations need to be made
     * @param entries - list of objects for which calculations need to be made
     * @param fields - fields that determine the path to the calculated value
     * @return double - returns a calculated value
     */
    private static double calculate(String function, List<JSONObject> entries, JSONArray fields) {

        // Convert field values to a list of values
        List<Double> values = entries.stream()
                .map(entry -> {
                    if (fields.length() == 1) {
                        Object value = entry.get(fields.getString(0));
                        return ((Number) value).doubleValue();
                    } else if (fields.length() == 2) {
                        Object value = entry.getJSONObject(fields.getString(0)).get(fields.getString(1));
                        return ((Number) value).doubleValue();
                    } else {
                        return null;
                    }
                })
                .toList();

        // Calculate result based on function
        return switch (function) {
            case "min" -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            case "max" -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            case "sum" -> values.stream().mapToDouble(Double::doubleValue).sum();
            case "average" -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            default -> throw new IllegalArgumentException("Unknown function: " + function);
        };
    }
}
