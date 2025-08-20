package models;

import enums.InstanceKind;

import java.util.List;

public class Instance {
    private InstanceKind kind;
    private String className;
    private String name;
    private List<Arg> args;

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