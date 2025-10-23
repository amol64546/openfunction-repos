package enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Kind {

    OBJECT("object"),
    PRIMITIVE("primitive"),
    STRING("string"),
    STRINGBUILDER("stringbuilder"),
    STRINGBUFFER("stringbuffer"),
    MAP("map"),
    LIST("list"),
    SET("set"),
    ARRAY("array"),
    ;

    private final String value;

    Kind(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Kind fromValue(String value) {
        return Kind.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
