package models;

import java.util.List;

public class Arg {
    public String kind;           // e.g., "object", "primitive", "string", "stringbuilder", "stringbuffer", "map", "list", "set", "array"

    // primitive/string
    public String type;           // e.g., "int", "long", "double", "boolean", "string"
    public String value;          // value as string

    // object-from-base64
    public String className;      // class of the nested object
    public String base64;         // base64 of the nested object
    public List<Arg> elements; // for arrays/collections: element args (can be primitive/object/etc)


}