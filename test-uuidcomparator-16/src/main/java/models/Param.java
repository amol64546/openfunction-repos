package models;

import enums.InstanceKind;

import java.util.List;

public class Param {
    private InstanceKind kind;
    private String className;
    private String name;
    private String base64;
    private List<Arg> args;

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }
    public InstanceKind getKind() {
        return kind;
    }

    public void setKind(InstanceKind kind) {
        this.kind = kind;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Arg> getArgs() {
        return args;
    }

    public void setArgs(List<Arg> args) {
        this.args = args;
    }
}