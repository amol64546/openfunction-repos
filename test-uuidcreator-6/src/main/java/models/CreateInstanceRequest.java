package models;

import enums.InstanceKind;

import java.util.List;

public class CreateInstanceRequest {

    private InstanceKind kind;
    private String className;
    private List<Arg> args;
    private Instance instance;
    private String name;

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

    public List<Arg> getArgs() {
        return args;
    }

    public void setArgs(List<Arg> args) {
        this.args = args;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
