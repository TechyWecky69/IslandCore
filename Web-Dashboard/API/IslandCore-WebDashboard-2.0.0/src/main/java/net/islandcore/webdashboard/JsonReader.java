package net.islandcore.webdashboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny, dependency-free JSON parser. Only used for reading trade log files
 * off disk (IslandCore itself doesn't ship a JSON library on the plugin
 * classpath, so this avoids adding a runtime dependency for a handful of
 * small, trusted files).
 *
 * Objects parse to {@code Map<String,Object>}, arrays to {@code List<Object>},
 * strings to {@code String}, numbers to {@code Double}, booleans to
 * {@code Boolean}, and null to {@code null}.
 */
final class JsonReader {
    private final String s;
    private int i;

    private JsonReader(String s) {
        this.s = s;
    }

    static Object parse(String text) {
        JsonReader r = new JsonReader(text);
        r.skipWs();
        Object value = r.readValue();
        r.skipWs();
        return value;
    }

    private Object readValue() {
        skipWs();
        if (i >= s.length()) throw new IllegalArgumentException("Unexpected end of JSON");
        char c = s.charAt(i);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't', 'f' -> readBoolean();
            case 'n' -> readNull();
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{');
        skipWs();
        if (peek() == '}') {
            i++;
            return map;
        }
        while (true) {
            skipWs();
            String key = readString();
            skipWs();
            expect(':');
            Object value = readValue();
            map.put(key, value);
            skipWs();
            char c = next();
            if (c == '}') break;
            if (c != ',') throw new IllegalArgumentException("Expected ',' or '}' at position " + i);
        }
        return map;
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWs();
        if (peek() == ']') {
            i++;
            return list;
        }
        while (true) {
            list.add(readValue());
            skipWs();
            char c = next();
            if (c == ']') break;
            if (c != ',') throw new IllegalArgumentException("Expected ',' or ']' at position " + i);
        }
        return list;
    }

    private String readString() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') break;
            if (c == '\\') {
                char esc = next();
                switch (esc) {
                    case '"' -> b.append('"');
                    case '\\' -> b.append('\\');
                    case '/' -> b.append('/');
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r');
                    case 't' -> b.append('\t');
                    case 'u' -> {
                        String hex = s.substring(i, i + 4);
                        i += 4;
                        b.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw new IllegalArgumentException("Bad escape at position " + i);
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private Double readNumber() {
        int start = i;
        if (peek() == '-') i++;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.'
                || s.charAt(i) == 'e' || s.charAt(i) == 'E' || s.charAt(i) == '+' || s.charAt(i) == '-')) {
            i++;
        }
        return Double.parseDouble(s.substring(start, i));
    }

    private Boolean readBoolean() {
        if (s.startsWith("true", i)) {
            i += 4;
            return Boolean.TRUE;
        }
        if (s.startsWith("false", i)) {
            i += 5;
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Bad literal at position " + i);
    }

    private Object readNull() {
        if (s.startsWith("null", i)) {
            i += 4;
            return null;
        }
        throw new IllegalArgumentException("Bad literal at position " + i);
    }

    private void skipWs() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private char peek() {
        if (i >= s.length()) throw new IllegalArgumentException("Unexpected end of JSON");
        return s.charAt(i);
    }

    private char next() {
        if (i >= s.length()) throw new IllegalArgumentException("Unexpected end of JSON");
        return s.charAt(i++);
    }

    private void expect(char c) {
        char got = next();
        if (got != c) throw new IllegalArgumentException("Expected '" + c + "' but got '" + got + "' at position " + (i - 1));
    }
}
