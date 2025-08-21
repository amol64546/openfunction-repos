package models;

import enums.Kind;
import enums.Type;

import java.util.List;

public class Arg {
    private Kind kind;

    // primitive
    private Type type;
    private String value;

    // object-from-base64
    private String className;
    private String base64;
    private List<Arg> elements;

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }

    public List<Arg> getElements() {
        return elements;
    }

    public void setElements(List<Arg> elements) {
        this.elements = elements;
    }
}