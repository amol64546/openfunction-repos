package models;

import java.util.List;

public class CreateInstanceRequest {
    public String kind; // e.g., "factory", "constructor"
    public String className;        // fully qualified name
    public List<Arg> args;            // constructor args // used if no factory given
    public Instance instance;                // optional
    public String name;       // e.g., "of", "from", "valueOf", "getInstance"


}
