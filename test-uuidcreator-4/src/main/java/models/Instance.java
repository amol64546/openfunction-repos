package models;

import java.util.List;

public class Instance {
    public String kind; // e.g., "factory", "constructor"
    public String className; // optional; defaults to targetClass
    public String name;       // e.g., "of", "from", "valueOf", "getInstance"
    public List<Arg> args;
}