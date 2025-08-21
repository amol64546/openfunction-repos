package enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;


public enum InstanceKind {

    OBJECT("object"),
    FACTORY("factory"),
    CONSTRUCTOR("constructor");

    private final String value;

    InstanceKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static InstanceKind fromValue(String value) {
        return InstanceKind.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
