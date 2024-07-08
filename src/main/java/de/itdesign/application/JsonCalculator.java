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
                // Чтение JSON файлов и Парсинг содержимого JSON файлов в объекты
                JSONObject dataJson = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE))));
                JSONObject operationsJson = new JSONObject(new String(Files.readAllBytes(Paths.get(OPERATIONS_FILE))));

                // Извлечение массивов из JSON объектов
                JSONArray data = dataJson.getJSONArray("entries");
                JSONArray operations = operationsJson.getJSONArray("operations");

                // Обработка данных и операций
                JSONArray results = processing(data, operations);

                // Запись результатов в JSON файл
                Files.write(Paths.get(OUTPUT_FILE), results.toString(2).getBytes());
            } catch (IOException e) {
                System.out.println("Error processing: " + e.getMessage());
            }
        } else {
            System.exit(1);
        }
    }

    // Метод для обработки данных и операций
    static JSONArray processing(JSONArray entries, JSONArray operations) {
        JSONArray results = new JSONArray();

        // Итерация по операциям
        IntStream.range(0, operations.length()).mapToObj(operations::getJSONObject).forEach(operation -> {
            String name = operation.optString("name");
            String function = operation.optString("function");
            String filter = operation.optString("filter");
            JSONArray fields = operation.optJSONArray("field");
            if (fields == null) {
                throw new IllegalArgumentException("Operation must have a field array");
            }

            // Компиляция регулярного выражения для фильтрации
            Pattern pattern = Pattern.compile(filter);
            List<JSONObject> filteredEntries = IntStream.range(0, entries.length())
                    .mapToObj(entries::getJSONObject)
                    .filter(entry -> pattern.matcher(entry.getString("name")).matches())
                    .collect(Collectors.toList());

            // Вычисление результата
            double result = calculate(function, filteredEntries, fields);

            // Создание объекта результата и добавление в массив результатов
            JSONObject resultObject = new JSONObject();
            resultObject.put("name", name);
            resultObject.put("roundedValue", formatDouble(result));
            results.put(resultObject);
        });

        return results;
    }

    // Форматирование добавляемого значения
    private static String formatDouble(double value) {
        DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(value);
    }

    // Метод для вычисления результата операции
    private static double calculate(String function, List<JSONObject> entries, JSONArray fields) {

        // Преобразование значений полей в список значений
        List<Double> values = entries.stream()
                .map(entry -> IntStream.range(0, fields.length())
                        .mapToDouble(i -> {
                            String[] path = fields.getString(i).split("\\.");
                            Object value = entry;
                            for (String key : path) {
                                value = ((JSONObject) value).get(key);
                            }
                            return ((Number) value).doubleValue();
                        })
                        .sum())
                .toList();

        // Вычисление результата на основе функции
        return switch (function) {
            case "min" -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            case "max" -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            case "sum" -> values.stream().mapToDouble(Double::doubleValue).sum();
            case "average" -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            default -> throw new IllegalArgumentException("Unknown function: " + function);
        };
    }

    // Метод для извлечения значений полей из JSON объекта
//    private static double fieldsToDouble(JSONObject entry, JSONArray fields) {
//        return IntStream.range(0, fields.length())
//                .mapToDouble(i -> {
//                    String[] path = fields.getString(i).split("\\.");
//                    Object value = entry;
//                    for (String key : path) {
//                        value = ((JSONObject) value).get(key);
//                    }
//                    return ((Number) value).doubleValue();
//                })
//                .sum();
//    }
}
