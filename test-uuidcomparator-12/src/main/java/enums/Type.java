package enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Type {
    INT("int"),
    LONG("long"),
    DOUBLE("double"),
    FLOAT("float"),
    SHORT("short"),
    BYTE("byte"),
    CHAR("char"),
    BOOLEAN("boolean");

    private final String value;

    Type(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Type fromValue(String value) {
        return Type.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
