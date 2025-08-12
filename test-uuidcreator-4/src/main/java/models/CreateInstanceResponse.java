package models;

public class CreateInstanceResponse {
    public String className;
    public String base64;

    public CreateInstanceResponse(String className, String base64) {
        this.className = className;
        this.base64 = base64;
    }
}
