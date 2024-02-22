package com.fool.maven.plugin;

public class ApiParameter {

    private String name;

    private Class<?> type;

    private String[] genericType;

    private String description;

    private String position;

    public ApiParameter() {
    }

    public ApiParameter(String name, Class<?> type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public ApiParameter(String name, Class<?> type, String description, String position) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.position = position;
    }

    public ApiParameter(String name, Class<?> type, String[] genericType, String description, String position) {
        this.name = name;
        this.type = type;
        this.genericType = genericType;
        this.description = description;
        this.position = position;
    }

    public String[] getGenericType() {
        return genericType;
    }

    public void setGenericType(String[] genericType) {
        this.genericType = genericType;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
