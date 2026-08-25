package io.vessel.web.json;

import io.vessel.web.VesselWebException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSerializerTest {

    record Point(int x, int y) {
    }

    record Line(Point start, Point end) {
    }

    record Named(String name) {
    }

    static class LegacyUser {
        private final long id;
        private final String name;

        LegacyUser(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isActive() {
            return true;
        }
    }

    static class NoGetters {
        private final int value = 1;

        int rawValue() {
            return value;
        }
    }

    @Test
    void serializesScalarsAndCollections() {
        assertEquals("\"hello\"", JsonSerializer.serialize("hello"));
        assertEquals("42", JsonSerializer.serialize(42));
        assertEquals("true", JsonSerializer.serialize(true));
        assertEquals("null", JsonSerializer.serialize(null));
        assertEquals("[1,2,3]", JsonSerializer.serialize(List.of(1, 2, 3)));
    }

    @Test
    void serializesARecordUsingItsComponentNames() {
        assertEquals("{\"x\":1,\"y\":2}", JsonSerializer.serialize(new Point(1, 2)));
    }

    @Test
    void serializesANestedRecord() {
        var line = new Line(new Point(0, 0), new Point(1, 1));

        assertEquals("{\"start\":{\"x\":0,\"y\":0},\"end\":{\"x\":1,\"y\":1}}", JsonSerializer.serialize(line));
    }

    @Test
    void serializesAListOfRecords() {
        assertEquals("[{\"x\":1,\"y\":2}]", JsonSerializer.serialize(List.of(new Point(1, 2))));
    }

    @Test
    void escapesSpecialCharactersInStrings() {
        assertEquals("\"line1\\nline2\\t\\\"quoted\\\"\"", JsonSerializer.serialize("line1\nline2\t\"quoted\""));
    }

    @Test
    void serializesAMapWithStringKeysInInsertionOrder() {
        var map = new LinkedHashMap<String, Object>();
        map.put("a", 1);
        map.put("b", "two");

        assertEquals("{\"a\":1,\"b\":\"two\"}", JsonSerializer.serialize(map));
    }

    @Test
    void rejectsAMapWithNonStringKeys() {
        var map = new LinkedHashMap<Integer, String>();
        map.put(1, "one");

        var exception = assertThrows(VesselWebException.class, () -> JsonSerializer.serialize(map));

        assertTrue(exception.getMessage().contains("non-String key '1'"));
    }

    @Test
    void serializesAPlainGetterBasedObjectSortedByPropertyName() {
        assertEquals("{\"active\":true,\"id\":1,\"name\":\"Alice\"}", JsonSerializer.serialize(new LegacyUser(1, "Alice")));
    }

    @Test
    void serializesAListOfGetterBasedObjects() {
        assertEquals("[{\"active\":true,\"id\":1,\"name\":\"Alice\"}]",
                JsonSerializer.serialize(List.of(new LegacyUser(1, "Alice"))));
    }

    @Test
    void rejectsSerializingAnObjectWithNoGettersAtAll() {
        var exception = assertThrows(VesselWebException.class, () -> JsonSerializer.serialize(new NoGetters()));

        assertTrue(exception.getMessage().contains("no getters found"));
    }

    @Test
    void serializesAMapWhoseValuesAreGetterBasedObjects() {
        var map = new LinkedHashMap<String, Object>();
        map.put("owner", new LegacyUser(1, "Alice"));

        assertEquals("{\"owner\":{\"active\":true,\"id\":1,\"name\":\"Alice\"}}", JsonSerializer.serialize(map));
    }

    @Test
    void deserializesARecord() {
        var point = JsonSerializer.deserialize("{\"x\":1,\"y\":2}", Point.class);

        assertEquals(new Point(1, 2), point);
    }

    @Test
    void deserializesANestedRecord() {
        var line = JsonSerializer.deserialize("{\"start\":{\"x\":0,\"y\":0},\"end\":{\"x\":1,\"y\":1}}", Line.class);

        assertEquals(new Line(new Point(0, 0), new Point(1, 1)), line);
    }

    @Test
    void deserializesAStringField() {
        assertEquals(new Named("Alice"), JsonSerializer.deserialize("{\"name\":\"Alice\"}", Named.class));
    }

    @Test
    void rejectsMalformedJson() {
        var exception = assertThrows(VesselWebException.class, () -> JsonSerializer.deserialize("{\"x\":1,", Point.class));

        assertTrue(exception.getMessage().contains("failed to parse JSON"));
    }

    @Test
    void rejectsAMissingPrimitiveFieldWithAClearMessage() {
        var exception = assertThrows(VesselWebException.class,
                () -> JsonSerializer.deserialize("{\"x\":1}", Point.class));

        assertTrue(exception.getMessage()
                .contains("missing required field 'y' for record 'Point' — int is a primitive type and cannot be null"));
    }

    @Test
    void rejectsATypeMismatchBetweenJsonAndTheTargetRecord() {
        var exception = assertThrows(VesselWebException.class,
                () -> JsonSerializer.deserialize("\"just a string\"", Point.class));

        assertTrue(exception.getMessage().contains("expected a JSON object, found a string"));
    }
}
