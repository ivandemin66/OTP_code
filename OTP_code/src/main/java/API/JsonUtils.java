package API;

// Утилитарный класс для извлечения значений из JSON-строк
public class JsonUtils {
    // Извлекает строковое значение по ключу
    public static String extractString(String json, String key) {
        String pattern = String.format("\\\"%s\\\":\\\"([^\\\"]*)\\\"", key);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : null;
    }
    // Извлекает целое число по ключу
    public static Integer extractInt(String json, String key) {
        String pattern = String.format("\\\"%s\\\":(\\d+)", key);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
    // Извлекает long по ключу
    public static Long extractLong(String json, String key) {
        String pattern = String.format("\\\"%s\\\":(\\d+)", key);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }
} 