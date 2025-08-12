package models;

import java.util.List;

public class Param {
    public String kind; // e.g., "object", "factory", "constructor", "primitive", "string", "stringbuilder", "stringbuffer", "map", "list", "set", "array"
    public String className;
    public String name;       // e.g., "of", "from", "valueOf", "getInstance"
    public List<Arg> args;
}