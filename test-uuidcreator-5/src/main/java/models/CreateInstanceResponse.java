package models;

public class CreateInstanceResponse {
    private String className;
    private String base64;

    public CreateInstanceResponse(String className, String base64) {
        this.className = className;
        this.base64 = base64;
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
}
