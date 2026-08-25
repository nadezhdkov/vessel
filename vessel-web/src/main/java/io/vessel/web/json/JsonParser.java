package io.vessel.web.json;

import io.vessel.web.VesselWebException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal recursive-descent JSON parser. Produces a generic tree —
 * {@code Map<String,Object>} for objects, {@code List<Object>} for arrays,
 * {@code String}/{@code Double}/{@code Long}/{@code Boolean}/{@code null}
 * for scalars — that {@link JsonSerializer} then binds into a target Java
 * type. Deliberately not exported: this is JsonSerializer's implementation
 * detail, not a public parsing API.
 */
final class JsonParser {

    private final String json;
    private int pos;

    private JsonParser(String json) {
        this.json = json;
    }

    static Object parse(String json) {
        var parser = new JsonParser(json);
        parser.skipWhitespace();
        var value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos != json.length()) {
            throw new VesselWebException(
                    "unexpected trailing content in JSON at position %d: '%s'"
                            .formatted(parser.pos, json.substring(parser.pos)));
        }
        return value;
    }

    private Object parseValue() {
        if (pos >= json.length()) {
            throw new VesselWebException("unexpected end of JSON input");
        }
        return switch (peek()) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        expect('{');
        skipWhitespace();
        var result = new LinkedHashMap<String, Object>();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            var key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            result.put(key, parseValue());
            skipWhitespace();
            char next = peek();
            pos++;
            if (next == '}') {
                return result;
            }
            if (next != ',') {
                throw new VesselWebException(
                        "expected ',' or '}' in JSON object at position %d, found '%c'".formatted(pos - 1, next));
            }
        }
    }

    private List<Object> parseArray() {
        expect('[');
        skipWhitespace();
        var result = new ArrayList<>();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            result.add(parseValue());
            skipWhitespace();
            char next = peek();
            pos++;
            if (next == ']') {
                return result;
            }
            if (next != ',') {
                throw new VesselWebException(
                        "expected ',' or ']' in JSON array at position %d, found '%c'".formatted(pos - 1, next));
            }
        }
    }

    private String parseString() {
        expect('"');
        var builder = new StringBuilder();
        while (true) {
            if (pos >= json.length()) {
                throw new VesselWebException("unterminated string in JSON input");
            }
            char c = json.charAt(pos++);
            if (c == '"') {
                return builder.toString();
            }
            if (c == '\\') {
                builder.append(parseEscape());
            } else {
                builder.append(c);
            }
        }
    }

    private char parseEscape() {
        if (pos >= json.length()) {
            throw new VesselWebException("unterminated escape sequence in JSON string");
        }
        char escaped = json.charAt(pos++);
        return switch (escaped) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> parseUnicodeEscape();
            default -> throw new VesselWebException("invalid escape sequence '\\%c' in JSON string".formatted(escaped));
        };
    }

    private char parseUnicodeEscape() {
        if (pos + 4 > json.length()) {
            throw new VesselWebException("incomplete \\u escape sequence in JSON string");
        }
        var hex = json.substring(pos, pos + 4);
        pos += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new VesselWebException("invalid \\u escape sequence in JSON string: '%s'".formatted(hex));
        }
    }

    private Boolean parseBoolean() {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (json.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new VesselWebException("invalid literal in JSON input at position %d".formatted(pos));
    }

    private Object parseNull() {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new VesselWebException("invalid literal in JSON input at position %d".formatted(pos));
    }

    private Number parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
            pos++;
        }
        boolean isFloatingPoint = false;
        if (pos < json.length() && json.charAt(pos) == '.') {
            isFloatingPoint = true;
            pos++;
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }
        }
        if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
            isFloatingPoint = true;
            pos++;
            if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }
        }
        if (pos == start) {
            throw new VesselWebException(
                    "invalid literal in JSON input at position %d: '%c'".formatted(pos, peek()));
        }
        var literal = json.substring(start, pos);
        return isFloatingPoint ? Double.parseDouble(literal) : Long.parseLong(literal);
    }

    private char peek() {
        if (pos >= json.length()) {
            throw new VesselWebException("unexpected end of JSON input");
        }
        return json.charAt(pos);
    }

    private void expect(char expected) {
        if (pos >= json.length() || json.charAt(pos) != expected) {
            throw new VesselWebException(
                    "expected '%c' in JSON input at position %d".formatted(expected, pos));
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
    }
}
